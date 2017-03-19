INTRODUCTION
============

This is an android app that computes for the accident risk of a particular point in the map using
the DRIVER platform data. The android app will then depending on your location issue
and appropriate audible warning of various factors in your vicinity.

The whole system is comprised of an Android app (this repo) that serves
as the front-end and a backend service that computes for the risk factor and serves to query the
DRIVER platform data (https://github.com/jedld/roadsafe_service.git).

The DRIVER platform is a service provided by the Philippine Department of Transportation and Communications
with the partnership of World Bank and Grab and other companies for reporting and
exposing anonymized crash data. For more information on the DRIVER platfrom see here:

 https://github.com/WorldBank-Transport/DRIVER
 https://github.com/WorldBank-Transport/DRIVER/blob/master/doc/api-use-case-health-organization.md

LICENSE
=======

This app is provided free of charge and is opensource. If there are bugs you can fix it
yourself or submit an issue/pull request and I can investigate if I have time.

Please take a look at LICENSE.md for details.  

REQUIREMENTS
============

* Android Studio 2.3 properly setup
* Make sure you have the backend service working as well https://github.com/jedld/roadsafe_service.git

INSTALLATION
============


1. Open the project using Android studio.
2. Obtain a google maps API KEY for android and replace the one in AndroidManifest.xml (look for ENTER_GOOGLE_MAPS_KEY)
3. You may want to modify the URL in:

/home/jedld/workspace/RoadSafe/app/src/main/java/client/roadsafe/org/roadsafe/client/RoadSafeServiceClient.java

   so that it points to your running backend service instance

4. Compile and Run the app.
