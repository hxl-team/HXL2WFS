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

		String insert = "<?xml version=\"1.0\"?>" +
				"<wfs:Transaction version=\"1.0.0\" handle=\"TX01\" service=\"WFS\" xmlns=\"http://www.example.com/myns\" " +
				"xmlns:myns=\"http://www.example.com/myns\" xmlns:gml=\"http://www.opengis.net/gml\" " +
				"xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >" +
				"<wfs:Insert handle=\"INSERT01\" >";

		while (results.hasNext()) {
			QuerySolution s = results.nextSolution();
			
			// TODO: this part needs to be updated once we have agreed on the fields we want to have:
			insert += "<myns:Mali_Wetlands fid=\""+results.getRowNumber()+"\" xmlns:myns=\"http://www.example.com/myns\">" +
					"         <myns:NAME>"+s.getLiteral("refName").getString()+"</myns:NAME>" +
					"         <myns:ISO>Test</myns:ISO>" +
					"         <myns:Country>"+s.getLiteral("country").getString()+"</myns:Country>" +
					"         <myns:F_CODE_DES>"+s.getLiteral("pcode").getString()+"</myns:F_CODE_DES>" +
					"         <myns:HYC_DESCRI>Some description</myns:HYC_DESCRI>" +
					"         <myns:SHAPE>";
			
			insert += wkt2gml(s.getLiteral("wkt").getString());
			
			insert += "</myns:SHAPE>" +
					"</myns:Mali_Wetlands>";
		}
		
		e.close();
		
		insert += "</wfs:Insert>" +
				"</wfs:Transaction>";

		return insert;
	}

	private String getSPARQLquery(String container) {
		return "prefix hxl: <http://hxl.humanitarianresponse.info/ns/#> "
				+ "prefix ogc: <http://www.opengis.net/ont/geosparql#> "
				+ "	SELECT DISTINCT ?unit ?level ?refName ?pcode ?wkt ?country WHERE { "
				+ "		GRAPH <" + container + ">{"
				+ "			?unit a hxl:AdminUnit ; " 
				+ "				  hxl:atLevel ?level ; "
				+ "				  hxl:featureRefName ?refName ; "
				+ "				  hxl:pcode ?pcode ; "
				+ "               hxl:atLocation ?c ;"
				+ "				  ogc:hasGeometry ?geom . "
				+ "			?geom ogc:hasSerialization ?wkt . " 
				+ "      }  " 
				+ "     ?c hxl:featureRefName ?country . " 
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
