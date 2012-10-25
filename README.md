# HXL2WFS

A Java program that takes HXL (Humanitarian eXchange Language) admin units from the HXL triple store (`http://hxl.humanitarianresponse.info/sparql`) and uploads them to an OGC Web Feature Server. 

This can then be fired at a WFS (OGC WFS-T) compliant server. Currently tested against a ArcGIS Server instance.

# Quickstart

Run HXL2WFSPusher with two arguments:

* The HXL data container that contains the admin units to be converted.

* The address of the transactional WFS to write to.

Currently, the feature properties are tied to a sample WFS setup, which has a certain set of properties set up. These do not make a lot of sense for HXL data, so this is something that needs to be changed once the we have agreed on the properties to have on the WFS end.

