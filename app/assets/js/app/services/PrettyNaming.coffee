"use strict"
ngDefine "services.PrettyNaming", (module) ->

  module.factory "PrettyNaming", () ->

    lookup = {
      "http://www.w3.org/2002/07/owl#Class": {
        name: "Class"
        code: "class"
      }
      "http://www.w3.org/2000/01/rdf-schema#Class": {
        name: "RDF Class"
        code: "class"
      }
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property": {
        name: "RDF Property"
        code: "property"
      }
      "http://www.w3.org/2002/07/owl#ObjectProperty": {
        name: "Object Property"
        code: "property"
      }
      "http://www.w3.org/2002/07/owl#Ontology": {
        name: "Ontology"
        code: "ontology"
      }
      "http://www.w3.org/2002/07/owl#Thing": {
        name: "Thing"
        code: "thing"
      }
      "__INSTANCE__": {
        name: "Individual"
        code: "instance"
      }
    }

    api = {
      list: () -> angular.copy(lookup)
      for: (uglyName) -> lookup[uglyName]?.name || uglyName
      classNameFor: (uglyName) -> lookup[uglyName]?.code || uglyName
    }
    api