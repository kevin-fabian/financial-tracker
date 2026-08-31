



## Sequenced Collections

Java 21 introduced `SequencedCollection`, `SequencedSet`, and `SequencedMap`.
Use the APIs when code needs explicit first/last ordering.

var first = list.getFirst();
var last = list.getLast();

For a sequenced collection:

collection.addFirst(item);
collection.addLast(item);

For maps:

var firstEntry = map.firstEntry();
var lastEntry = map.lastEntry();

Do not introduce these APIs when ordinary collection operations are clearer.