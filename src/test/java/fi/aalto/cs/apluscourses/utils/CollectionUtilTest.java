package fi.aalto.cs.apluscourses.utils;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CollectionUtilTest {

  @Test
  void testMapWithIndex() {
    List<String> source = List.of("a", "b", "c");
    List<String> result =
        CollectionUtil.mapWithIndex(source, (item, index) -> item + index.toString(), 4);
    MatcherAssert.assertThat(result, is(List.of("a4", "b5", "c6")));
  }

  @Test
  void testIndexOf() {
    Object item = new Object();
    Iterator<Object> it = List.of(new Object(), new Object(), item, new Object()).iterator();
    Assertions.assertEquals(2, CollectionUtil.indexOf(it, item));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testRemoveIf() {
    Consumer<String> callback = mock(Consumer.class);

    Collection<String> collection = new ArrayDeque<>();
    collection.add("Audi");
    collection.add("BMW");
    collection.add("Chevrolet");
    collection.add("Daimler");
    collection.add("Alfa Romeo");
    collection.add("Bentley");
    collection.add("Chrysler");
    collection.add("Dodge");

    var removed = CollectionUtil.removeIf(collection, s -> s.startsWith("C"));
    MatcherAssert.assertThat(removed, hasItem("Chevrolet"));
    MatcherAssert.assertThat(removed, hasItem("Chrysler"));

    Assertions.assertEquals(6, collection.size());
    MatcherAssert.assertThat(collection, hasItem("Audi"));
    MatcherAssert.assertThat(collection, hasItem("BMW"));
    MatcherAssert.assertThat(collection, hasItem("Daimler"));
    MatcherAssert.assertThat(collection, hasItem("Alfa Romeo"));
    MatcherAssert.assertThat(collection, hasItem("Bentley"));
    MatcherAssert.assertThat(collection, hasItem("Dodge"));
  }
}
