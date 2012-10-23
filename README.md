# HXL2WFS

A Java program that takes HXL (Humanitarian eXchange Language) admin units from the HXL triple store (`http://hxl.humanitarianresponse.info/sparql`) and uploads them to an OGC Web Feature Server. 

This can then be fired at a WFS (OGC WFS-T) compliant server. Currently tested against a ArcGIS Server instance.

# Quickstart

Run HXL2WFSPusher with two arguments:
1. The HXL data container that contains the admin units to be converted.
2. The address of the transactional WFS to write to.