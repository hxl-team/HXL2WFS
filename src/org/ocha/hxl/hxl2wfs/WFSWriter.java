package org.ocha.hxl.hxl2wfs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class WFSWriter {

	private String wfsaddress;

	public WFSWriter(String wfsaddress) {
		this.wfsaddress = wfsaddress;
	}

	/*
	 * Performs the post request to the WFS
	 */
	public void insert(String wfsInsert) {
		try {

			URL url = new URL(this.wfsaddress);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type",
					"text/xml");
			connection.setRequestProperty("Content-Length",
					String.valueOf(wfsInsert.length()));

			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream());
			writer.write(wfsInsert);
			writer.flush();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			String serverOutput = "";
			for (String line; (line = reader.readLine()) != null;) {
				serverOutput += line;
			}

			writer.close();
			reader.close();
			
			if(serverOutput.contains("<wfs:totalInserted>")){
				int start = serverOutput.indexOf("<wfs:totalInserted>")+19;
				int end = serverOutput.indexOf("</wfs:totalInserted>");
				System.out.println(serverOutput.substring(start, end) + " features added to the WFS.");
			}else{
				System.out.println("No features inserted. You have probably seen an error message somewhere above.");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
