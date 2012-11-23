package org.ocha.hxl.hxl2wfs;

import it.cutruzzula.lwkt.WKTParser;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class HXLReader {

	private String container;
	private String wfsns;
	private int featureCount = -1;
	private int currentOffset = 0;
	private static int limit = 1000;
	private static String service = "http://hxl.humanitarianresponse.info/sparql";

	public HXLReader(String container, String wfsns) {
		this.container = container;
		this.wfsns = wfsns;
	}

	// checks whether there are more results to come from this data container
	public boolean hasMoreResults() {
		return currentOffset <= getFeatureCount();
	}

	/*
	 * Returns a WFS Insert XML document
	 */
	public String getWFSInsert() {

		// query out all things in the container that are admin units,
		// along with their names, pcodes, geometries

		QueryExecution e = QueryExecutionFactory.sparqlService(service,
				getSPARQLquery(false));

		ResultSet results = e.execSelect();

		String insert = "<?xml version=\"1.0\"?>\n"
				+ "<wfs:Transaction version=\"1.0.0\" handle=\"TX01\" service=\"WFS\"\n"
				+ "xmlns=\"" + wfsns + "\"\n" + "xmlns:myns=\"" + wfsns
				+ "\"\n" + "xmlns:gml=\"http://www.opengis.net/gml\"\n"
				+ "xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
				+ "xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
				+ "<wfs:Insert handle=\"INSERT01\">\n";

		while (results.hasNext()) {
			QuerySolution s = results.nextSolution();

			// the feature type depends on the country code and the admin level,
			// eg NERAdmin_0
			// BUT: populated places and APLs don't have level info, so:
			
			String type = s.getResource("type").getLocalName();
			
			String featureType = ""; 
			String country = s.getLiteral("countryCode").getString();
			
			if(type.equals("PopulatedPlace")){
				featureType = country + "_PP";
			}else if (type.equals("APL")){
				featureType = country + "_APL";
			}else{ // Admin Unit
				String adminLevel = s.getResource("level").toString()
						.split("/adminlevel")[1];
				featureType = country + "Admin_"
						+ adminLevel;
			} 
			
			insert += "<myns:" + featureType + " fid=\""
					+ results.getRowNumber() + "\" xmlns:myns=\"" + wfsns
					+ "\">\n" + "<myns:Name>"
					+ s.getLiteral("featureName").getString()
					+ "</myns:Name>\n" + "<myns:Valid_on>"
					+ this.formatDate(s.getLiteral("date").getString())
					+ "</myns:Valid_on>\n" + "<myns:ReferenceName>"
					+ s.getLiteral("refName").getString()
					+ "</myns:ReferenceName>\n" + "<myns:pcode>"
					+ s.getLiteral("pcode").getString() + "</myns:pcode>\n"
					+ "<myns:SHAPE>" + wkt2gml(s.getLiteral("wkt").getString())
					+ "</myns:SHAPE>\n" + "</myns:" + featureType + ">\n";
		}

		e.close();

		insert += "</wfs:Insert>\n";
		insert += "</wfs:Transaction>\n";

		return insert;
	}

	private String formatDate(String date) {
		if(date.length() == 10){
			// only the data is given, add timestamp to make ArcGIS Server happy:
			return date+"T00:00:00.0Z";
		}else{
			// we assume all is good:
			return date;
		}
	}

	// returns the number of features in the data container that will be
	// inserted
	public int getFeatureCount() {

		// only do the query once:
		if (featureCount == -1) {
			QueryExecution e = QueryExecutionFactory.sparqlService(service,
					getSPARQLquery(true));

			ResultSet results = e.execSelect();

			while (results.hasNext()) {
				QuerySolution s = results.nextSolution();
				featureCount = s.getLiteral("features").getInt();
			}

			e.close();

		}

		return featureCount;
	}

	// returns the query for the next <em>limit>/em> features (after
	// <em>offset</em>)
	public String getSPARQLquery(boolean count) {
		String query = "prefix hxl: <http://hxl.humanitarianresponse.info/ns/#> \n "
				+ "prefix ogc: <http://www.opengis.net/ont/geosparql#> \n "
				+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n "
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n "
				+ "\n ";

		if (count) {
			query += "SELECT (COUNT(*) as ?features) WHERE {    \n ";
		} else {
			query += "SELECT DISTINCT ?unit ?type ?level ?featureName ?refName ?wkt ?pcode ?countryCode ?date WHERE {  \n ";
		}

		query += "   \n " + "  GRAPH <" + container + ">{\n " + "     \n "
				+ "    ?unit rdf:type ?type ; \n "
				+ "           hxl:featureName ?featureName ; \n "
				+ "           hxl:featureRefName ?refName ; \n "
				+ "           hxl:pcode ?pcode ; \n "
				+ "           ogc:hasGeometry ?geom . \n " + "     \n "
				+ "     ?geom ogc:hasSerialization ?wkt .         \n "
				+ "     \n " + "     OPTIONAL {\n " + "       \n "
				+ "     	?unit hxl:atLevel ?level .\n " + "       \n "
				+ "     }\n " + "     \n " + "   }\n " + "      \n "
				+ "   ?unit hxl:atLocation* ?c . \n " + "\n "
				+ "   ?c a hxl:Country ;\n "
				+ "      <http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/codeISO3> ?countryCode .\n " + "   \n "
				+ " \n "
				+ "   <" + container + "> hxl:validOn ?date .\n "
				+ " \n "
				+ "}";
		if (!count) {
			query += " ORDER BY ?unit LIMIT " + limit + " OFFSET "
					+ currentOffset;
			currentOffset += limit;
		}

		return query;
	}

	/**
	 * Converts WKT to GML
	 * 
	 * @param A
	 *            well-known-text string
	 * @return The input geometry encoded in the Geographic Markup Language
	 */
	private String wkt2gml(String wkt) {
		try {
			String gml = WKTParser.parseToGML2(wkt);
			return gml;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error: " + e.getMessage();
		}
	}
}
