# The Ontology Kickstarter

The Ontology Kickstarter is a web application which helps you start creating you ontology very quickly. It's like yeoman for ontologies.

While creating an ontology the ontologist determines keywords/key terms which is probably be included in the ontology. Ontologist searches these keywords on this application and she is presented with relevant entities that have already been defined somewhere on the web.

Ontologist may traverse within results discovering new entities which she never thought before. Ontologist selects the entities which she want to be included in her kickstart ontology, making even the initial ontology is connected with the whole semantic web.

The ontologist processes these results, selects the entities which she wants in her initial ontology and exports them & their connected entities into an ontology format of her desires.

## Research

Ontology reusing is my mSC research subject. I am developing an ontology reusing methodology which will make reusing step more defined and concrete. This tool is being developed to support the new methodology and make it easy to use.

Traversing within results in a creative UI and concept of exporting an ontology in triple basis instead of using the whole ontology are the two key things that this application provides.

## Technical

This application is developed using Play Framework 2.2 using Scala and Angular.js and Bootstrap. The whole list of technologies helped this project in [credits](#credits) section.

## Live Application

This application is not hosted in anywhere _yet_.

## Screenshots

**[Click here for all screenshots!](SCREENSHOTS.md)**

![TOK Screenshot](http://www.ubenzer.com/deepo/github/tezapp/1.png "TOK Screenshot")

## Running application on your machine

1. [Download](http://www.playframework.com/download) and install PlayFramework 2.2. Refer to [their documentation](http://www.playframework.com/documentation/2.2.x/Installing) if required.
2. Install [MongoDB 2.6](http://www.mongodb.org/)
3. Run MongoDB.
4. Create a copy of `conf/secret.sample.conf` naming it `conf/secret.conf`.
5. Edit `conf/secret.conf` and fill it with your own configuration.
5. Run application with `run` or `start` on Play console.

## Contributing

If you have any ideas, feature requests or if you found a bug please [open an issue](https://github.com/ubenzer/tezapp/issues/new).

If you want to help by showing your coding skills, create a [Pull Request](https://help.github.com/articles/creating-a-pull-request) [for an issue](https://github.com/ubenzer/tezapp/issues).

## Credits

These are the libraries, frameworks etc. that are used in this project.

### Backend
+ [Play Framework](http://www.playframework.com/)
+ [MongoDB](http://www.mongodb.org/)
+ [RDF I/O](http://www.openrdf.org/)
+ [Reactive Mongo](http://reactivemongo.org/)

### Frontend
+ [Angular.js](http://angularjs.org/)
+ [Blob.js](https://github.com/eligrey/Blob.js/)
+ [FileSaver.js](https://github.com/eligrey/FileSaver.js/)
+ [Require.js](http://requirejs.org/)
+ [Bootstrap](http://getbootstrap.com/)
+ [ng-tags-input.js](http://mbenford.github.io/ngTagsInput/)
+ [ui-router](https://github.com/angular-ui/ui-router)
+ [ui-bootstrap](http://angular-ui.github.io/bootstrap/)
+ [ngDefine](http://nikku.github.io/requirejs-angular-define/)
+ [Font Awesome](http://fortawesome.github.io/Font-Awesome/)

### Data Sources
+ [Swoogle](http://swoogle.umbc.edu/)
+ [Sindice](http://sindice.com/)
+ [Watson](http://watson.kmi.open.ac.uk/WatsonWUI/)
