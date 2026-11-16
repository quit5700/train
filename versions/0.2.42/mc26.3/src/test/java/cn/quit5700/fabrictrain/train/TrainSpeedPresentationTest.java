package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainSpeedPresentationTest {
    @Test void showsReadableValuesAndPreservesUntouchedPrecision() {
        assertEquals("180.9", TrainSpeedPresentation.format(180.89999999999998));
        assertEquals("1000", TrainSpeedPresentation.format(1000));
        assertTrue(Double.isNaN(TrainSpeedPresentation.changedValue("180.9",180.89999999999998)));
        assertEquals(180.95, TrainSpeedPresentation.changedValue("180.95",180.89999999999998));
    }

    @Test void identifiesOutOfRangeAndInvalidInput() {
        assertEquals(TrainSpeedPresentation.InputError.OUT_OF_RANGE, TrainSpeedPresentation.inputError("10000"));
        assertEquals(TrainSpeedPresentation.InputError.OUT_OF_RANGE, TrainSpeedPresentation.inputError("-1"));
        assertEquals(TrainSpeedPresentation.InputError.INVALID_NUMBER, TrainSpeedPresentation.inputError("NaN"));
        assertEquals(TrainSpeedPresentation.InputError.INVALID_NUMBER, TrainSpeedPresentation.inputError(""));
        assertNull(TrainSpeedPresentation.inputError("1000"));
        assertNull(TrainSpeedPresentation.inputError("0"));
    }

    @Test void convertsActualDisplacementAtNominalTickRate() {
        assertEquals("1000", TrainSpeedPresentation.format(TrainSpeedPresentation.measuredSpeed(1000D/72)));
        assertEquals("0", TrainSpeedPresentation.format(TrainSpeedPresentation.measuredSpeed(0)));
        assertEquals(36, TrainSpeedPresentation.measuredSpeed(0.5));
    }
}
