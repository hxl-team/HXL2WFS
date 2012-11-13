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
	public String getWFSInsert(String container, String wfsns) {

		// query out all things in the container that are admin units,
		// along with their names, pcodes, geometries

		String service = "http://hxl.humanitarianresponse.info/sparql";

		QueryExecution e = QueryExecutionFactory.sparqlService(service,
				getSPARQLquery(container));

		ResultSet results = e.execSelect();
		

		String insert = "<?xml version=\"1.0\"?> \n"
				+ "<wfs:Transaction version=\"1.0.0\" handle=\"TX01\" service=\"WFS\" xmlns=\"" + wfsns + "\"  \n"
				+ "xmlns:myns=\"" + wfsns + "\" xmlns:gml=\"http://www.opengis.net/gml\"  \n"
				+ "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\"  \n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" > \n"
				+ "<wfs:Insert handle=\"INSERT01\" > \n";

		while (results.hasNext()) {
			QuerySolution s = results.nextSolution();

			// the feature type depends on the country code and the admin level, eg NERAdmin_0
			String adminLevel = s.getResource("level").toString().split("/adminlevel")[1];
			String featureType = s.getLiteral("countryCode")+"Admin_"+adminLevel;
			
			insert += "<myns:" + featureType + " fid=\"" + results.getRowNumber()
					+ "\" xmlns:myns=\"" + wfsns + "\"> \n"
					+ "         <myns:Name> \n"
					+ s.getLiteral("featureName").getString()
					+ "         </myns:Name> \n"
					+ "         <myns:ReferenceName> \n"
					+ s.getLiteral("refName").getString()
					+ "         </myns:ReferenceName> \n"
					+ "         <myns:pcode> \n"
					+ s.getLiteral("pcode").getString()
					+ "         </myns:pcode> \n"					
					+ "         <myns:SHAPE> \n";

			insert += wkt2gml(s.getLiteral("wkt").getString());

			insert += "</myns:SHAPE>" + "</myns:" + featureType + "> \n";
		}

		e.close();

		insert += "</wfs:Insert>" + "</wfs:Transaction> \n";

		return insert;
	}

	public String getSPARQLquery(String container) {
		return "prefix hxl: <http://hxl.humanitarianresponse.info/ns/#> \n "
+"prefix ogc: <http://www.opengis.net/ont/geosparql#> \n "
+"\n "
+"SELECT DISTINCT ?unit ?level ?featureName ?refName ?wkt ?pcode ?countryCode WHERE {  \n "
+"  { GRAPH <"+container+">{\n "
+"    ?unit a hxl:AdminUnit ; \n "
+"            hxl:atLevel ?level ; \n "
+"            hxl:featureName ?featureName ; \n "
+"            hxl:featureRefName ?refName ; \n "
+"            hxl:pcode ?pcode ; \n "
+"            ogc:hasGeometry ?geom . \n "
+"    \n "
+"    ?geom ogc:hasSerialization ?wkt . }                                                                                                                                                                                                                                                                                                                                                                                      \n "
+"    \n "
+"   ?unit hxl:atLocation+ ?c . \n "
+"   ?c a hxl:Country; \n "
+"        hxl:pcode ?countryCode . \n "
+"  \n "
+"  } UNION {\n "
+"  \n "
+"    GRAPH <"+container+">{\n "
+"	?unit a hxl:Country ;  \n "
+"            hxl:atLevel ?level ; \n "
+"            hxl:featureName ?featureName ; \n "
+"            hxl:featureRefName ?refName ;  \n "
+"            hxl:pcode ?pcode ;\n "
+"            hxl:pcode ?countryCode ; \n "
+"            ogc:hasGeometry ?geom . \n "
+"    \n "
+"    ?geom ogc:hasSerialization ?wkt . }\n "
+"\n "
+"  }                                                                                                                                                                                                                                                                                                                                                                                      \n "
+"    \n "
+"}\n ";

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
