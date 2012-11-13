package org.ocha.hxl.hxl2wfs;

import it.cutruzzula.lwkt.WKTParser;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class HXLReader {

	/*
	 * Returns a WFS Insert XML document
	 */
	public String getWFSInsert(String container) {

		// query out all things in the container that are admin units,
		// along with their names, pcodes, geometries

		String service = "http://hxl.humanitarianresponse.info/sparql";

		QueryExecution e = QueryExecutionFactory.sparqlService(service,
				getSPARQLquery(container));

		ResultSet results = e.execSelect();

		String insert = "<?xml version=\"1.0\"?>"
				+ "<wfs:Transaction version=\"1.0.0\" handle=\"TX01\" service=\"WFS\" xmlns=\"http://www.example.com/myns\" "
				+ "xmlns:myns=\"http://www.example.com/myns\" xmlns:gml=\"http://www.opengis.net/gml\" "
				+ "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >"
				+ "<wfs:Insert handle=\"INSERT01\" >";

		while (results.hasNext()) {
			QuerySolution s = results.nextSolution();

			// the feature type depends on the country code and the admin level, eg NERAdmin_0
			String adminLevel = s.getResource("level").toString().split("/adminlevel")[1];
			String featureType = s.getLiteral("countryCode")+"Admin_"+adminLevel;
			
			insert += "<myns:+featureType+ fid=\"" + results.getRowNumber()
					+ "\" xmlns:myns=\"http://www.example.com/myns\">"
					+ "         <myns:Name>"
					+ s.getLiteral("featureName").getString()
					+ "         </myns:Name>"
					+ "         <myns:ReferenceName>"
					+ s.getLiteral("refName").getString()
					+ "         </myns:ReferenceName>"
					+ "         <myns:pcode>"
					+ s.getLiteral("pcode").getString()
					+ "         </myns:pcode>"					
					+ "         <myns:SHAPE>";

			insert += wkt2gml(s.getLiteral("wkt").getString());

			insert += "</myns:SHAPE>" + "</myns:Mali_Wetlands>";
		}

		e.close();

		insert += "</wfs:Insert>" + "</wfs:Transaction>";

		return insert;
	}

	public String getSPARQLquery(String container) {
		return "prefix hxl: <http://hxl.humanitarianresponse.info/ns/#> "
				+ "prefix ogc: <http://www.opengis.net/ont/geosparql#> "
				+ "	SELECT DISTINCT ?unit ?level ?featureName ?refName ?pcode ?wkt ?countryCode WHERE { "
				+ "		GRAPH <"
				+ container
				+ ">{"
				+ "			?unit a hxl:AdminUnit ;"
				+ "            hxl:atLevel ?level ; "
				+ "            hxl:featureName ?featureName ; "
				+ "            hxl:featureRefName ?refName ; "
				+ "            hxl:pcode ?pcode ;            	"
				+ "            ogc:hasGeometry ?geom . 	"
				+ "    ?geom ogc:hasSerialization ?wkt .       }   "
				+ "                                                                                                                                                                                                                                                                                                                                                                                "
				+ "    ?unit hxl:atLocation+ ?c ." 
				+ "	   ?c a hxl:Country;"
				+ "       hxl:pcode ?countryCode . " 
				+ "}";
	}

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
