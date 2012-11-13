package org.ocha.hxl.hxl2wfs;

public class HXL2WFSPusher {

	/**
	 * @param args[0] container URI, e.g. http://hxl.humanitarianresponse.info/data/datacontainers/unocha/1344946260.924107
	 */
	public static void main(String[] args) {
		
		if(args.length != 2){
		
			System.out.println("Usage: java HXL2WFSPusher http://hxl.humanitarianresponse.info/data/my/datacontainer/uri http://some.transactional.wfs/uri ");
	
		}else{
			
			String container = args[0];
			String wfsaddress = args[1];
			
			System.out.println("Fetching features from HXL triple store...");

			HXLReader reader = new HXLReader();
			
			System.out.println(reader.getSPARQLquery(container));
			
			String insertCode = reader.getWFSInsert(container);

			System.out.println(insertCode);			
			System.out.println("Writing data to WFS...");
			
			WFSWriter writer = new WFSWriter(wfsaddress);
			writer.insert(insertCode);
			
			
//			for (String container : (new UpdateChecker().getUpdatedContainers())) {
//				System.out.println(container);
//			}
			
			System.out.println("Done. Bye bye.");
		}
		
	}

}
