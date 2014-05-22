__Schema Validation Utilities__

The `schema-utils` library is used by several [OGC](http://www.opengeospatial.org/) 
conformance test suites. It provides support for validating XML representations 
using the following schema languages: 

* W3C XML Schema 1.0
* Schematron (ISO/IEC 19757-3:2006)
* RELAX NG (ISO/IEC 19757-2:2008)

Visit the [project documentation website](http://opengeospatial.github.io/schema-utils/) 
for more information, including the API documentation.

__Note__

Apache Maven is required to build the project. Some dependencies are currently 
not available in the central repository. To obtain them, add the following remote 
repository to a profile in the Maven settings file (${user.home}/.m2/settings.xml).

    <profile>
      <id>ogc.cite</id>
      <!-- activate profile by default or explicitly -->
      <repositories>
        <repository>
          <id>opengeospatial-cite</id>
          <name>OGC CITE Repository</name>
          <url>https://svn.opengeospatial.org/ogc-projects/cite/maven</url>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
