Dashboard-Test
==============

This test tests the 'testFailureIgnore' and the CTest dashboard parameters.  It is
configured so that a failed test will not cause the build to fail (testFailureIgnore
is set to 'true').  The failed test is then reported to the dashboard set up for the
project at my.dash.org:

    http://my.cdash.org/index.php?project=cmake-maven-plugin

This is configured through the 'dashboard' parameter in this project's pom.xml.