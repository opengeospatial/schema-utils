# Release Notes

## 1.8 (2016-09-DD)
This release includes the following changes:

* SchematronValidator: add validation option to produce plain text output.
* SchematronValidator: enable default phase if phase is not specified ("#ALL" if no default phase).

## 1.7 (2015-06-24)
This release includes the following enhancements:

* SchematronValidator: accept DOMSource that wraps an Element node.
* SchematronValidator: activate all phases ("#ALL") if one is not specified.
* Add methods `getErrors()` and `addErrors(Collection<ValidationError>)` to 
ValidationErrorHandler.

## 1.6 (2015-04-28)
This release includes no functional changes. Several dependencies and Maven 
plugins have been updated. Accessing a special repository is no longer required.

## 1.5 (2014-05-20)
The project is now hosted at GitHub. This release introduces new site content, 
but the essential functionality of the library is unchanged.

* Modify POM for GitHub.
* Add new site content.
* Change license to Apache License, Version 2.0.

## 1.4 (2014-04-15)
This minor release includes the following updates:

* Fix Javadoc errors reported when building with JDK 8.
* Set system identifier for retrieved XML Schema source.
* Update dependencies to latest releases.
