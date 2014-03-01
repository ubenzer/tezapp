"use strict"
ngDefine "services.PrettyNaming", (module) ->

  module.factory "PrettyNaming", () ->

    prettyPrintLookup = {
      "http://www.w3.org/2002/07/owl#Class": "Class"
      "http://www.w3.org/2002/07/owl#ObjectProperty": "Object Property"
      "http://www.w3.org/2002/07/owl#Ontology": "Ontology"
      "http://www.w3.org/2002/07/owl#Thing": "Thing"
    }

    classNamingLookup = {
      "http://www.w3.org/2002/07/owl#Class": "class"
      "http://www.w3.org/2002/07/owl#ObjectProperty": "property object-property"
      "http://www.w3.org/2002/07/owl#Ontology": "ontology"
    }

    api = {
      for: (uglyName) -> prettyPrintLookup[uglyName] || uglyName
      classNameFor: (uglyName) -> classNamingLookup[uglyName] || uglyName
    }
    api