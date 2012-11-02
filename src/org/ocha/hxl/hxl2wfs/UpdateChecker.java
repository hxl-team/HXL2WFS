package org.ocha.hxl.hxl2wfs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class UpdateChecker {

	private static String checkPath = "lastChecked.txt";
	private static String service = "http://hxl.humanitarianresponse.info/sparql";

	/**
	 * 
	 * @return A String ArrayList containing the URIs of all data containers
	 *         (a.k.a. named graphs) that have changed in the last 24 hours.
	 */
	public ArrayList<String> getUpdatedContainers() {

		ArrayList<String> updatedContainers = new ArrayList<String>(0);

		QueryExecution e = QueryExecutionFactory.sparqlService(service,
				getSPARQLquery());

		ResultSet results = e.execSelect();
		
		while (results.hasNext()) {
			QuerySolution s = results.nextSolution();
			updatedContainers.add(s.getResource("container").getURI());					
		}

		saveLastCheckedDate();		
		return updatedContainers;
	}

	/**
	 * 
	 * @return  A SPARQL query that fetches all datacontainers containing countries, admin units,
	 * APLs, and populated places that have changed since the last check 
	 */
	private String getSPARQLquery() {
		return  "prefix dct: <http://purl.org/dc/terms/>\n" + 
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"prefix hxl: <http://hxl.humanitarianresponse.info/ns/#>\n" +
				"prefix ogc: <http://www.opengis.net/geosparql#>\n" +
				"	\n" +
				"SELECT DISTINCT ?container WHERE {\n" +
				"   GRAPH ?container {\n" +
				"         ?container dct:date ?date .\n" +        	 
				"         { { ?feature a hxl:Country . }  \n" +
				"           UNION\n" +
				"           { ?feature a hxl:AdminUnit . }\n" +
				"           UNION\n" +
				"           { ?feature a hxl:APL . }\n" +
				"           UNION\n" +
				"           { ?feature a hxl:PopulatedPlace . }\n" +
				"         } \n" +
				"   }\n" +
				"   FILTER (?date > \""+getLastCheckDate()+"\")\n" +
				"}";
	}

	/**
	 * 
	 * @return The timestamp (ISO encoded) this script was last ran (save in file lastChecked.txt), or, if the file is not present (e.g. when the script is ran for the first time) 
	 * it returns Januar 1st, 2000 
	 */
	private static String getLastCheckDate() {
		try {
			FileInputStream stream = new FileInputStream(new File(checkPath));
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
					fc.size());
			stream.close();
			return Charset.defaultCharset().decode(bb).toString();
		} catch (IOException e) {
			System.out
					.println("No file with timestamp of last check found. Assuming 2000-01-01.");
			return "2000-01-01T00:00:00+01:00";
		}
	}

	/**
	 * Saves the current timestamp to lastChecked.txt
	 */
	private void saveLastCheckedDate() {
		String now = new DateTime().toString(ISODateTimeFormat
				.dateTimeNoMillis());
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(checkPath));
			writer.write(now);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}