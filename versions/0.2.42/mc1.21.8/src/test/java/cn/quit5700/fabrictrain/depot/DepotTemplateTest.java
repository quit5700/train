package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.train.CarType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepotTemplateTest {
    @Test
    void defaultTemplateIsEmptyAndEmptyTemplateRoundTrips() {
        assertTrue(DepotTemplate.defaultTemplate().cars().isEmpty());
        assertEquals(new DepotTemplate(List.of()), DepotTemplate.decode(""));
    }

    @Test
    void orderedTemplateRoundTrips() {
        DepotTemplate template = new DepotTemplate(List.of(CarType.PASSENGER, CarType.CARGO, CarType.PASSENGER));
        assertEquals(template, DepotTemplate.decode(template.encode()));
    }

    @Test
    void rejectsMoreThanOneHundredCars() {
        assertThrows(IllegalArgumentException.class,
                () -> new DepotTemplate(java.util.Collections.nCopies(101, CarType.PASSENGER)));
    }
}
