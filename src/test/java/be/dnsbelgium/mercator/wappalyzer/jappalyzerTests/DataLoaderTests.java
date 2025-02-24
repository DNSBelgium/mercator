// copied from jappalyzer library
package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests;

import org.junit.Test;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Category;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.DataLoader;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Group;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Technology;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

public class DataLoaderTests {

    @Test
    public void shouldLoadOneGroup() {
        DataLoader loader = new DataLoader();
        List<Technology> technologies = loader.loadInternalTechnologies();
        Optional<Technology> technology = technologies.stream()
                .filter(item -> item.getName().equals("Gauges"))
                .findFirst();
        if (technology.isPresent()) {
            List<Category> categories = technology.get().getCategories();
            List<Group> groups = getCategoriesGroups(categories);
            assertThat(categories.size()).isEqualTo(1);
            assertThat(groups).containsExactlyInAnyOrder(new Group(8, "Analytics"));
        } else {
            fail("Technology is not found");
        }
    }

    @Test
    public void shouldLoadSeveralGroups() {
        DataLoader loader = new DataLoader();
        List<Technology> technologies = loader.loadInternalTechnologies();

        Optional<Technology> technology = technologies.stream()
                .filter(item -> item.getName().equals("Genesys Cloud"))
                .findFirst();
        if (technology.isPresent()) {
            List<Category> categories = technology.get().getCategories();
            List<Group> groups = getCategoriesGroups(categories);
            assertThat(categories.size()).isEqualTo(2);
            assertThat(groups).containsExactlyInAnyOrder(
                    new Group(2, "Marketing"),
                    new Group(4, "Communication"));
        } else {
            fail("Technology is not found");
        }
    }

    private List<Group> getCategoriesGroups(List<Category> categories) {
        return categories.stream()
                .map(Category::getGroups)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

}