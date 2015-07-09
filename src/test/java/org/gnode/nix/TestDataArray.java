package org.gnode.nix;

import org.gnode.nix.valid.Result;
import org.gnode.nix.valid.Validator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class TestDataArray {

    private File file;
    private Block block;
    private DataArray array1, array2;

    private Date statup_time;

    @Before
    public void setUp() {
        // precision of time_t is in seconds hence (millis / 1000) * 1000
        statup_time = new Date((System.currentTimeMillis() / 1000) * 1000);

        file = File.open("test_DataArray_" + UUID.randomUUID().toString() + ".h5", FileMode.Overwrite);

        block = file.createBlock("block_one", "dataset");
        array1 = block.createDataArray("array_one",
                "testdata",
                DataType.Double,
                new NDSize(new int[]{3 * 4 * 2}));
        array2 = block.createDataArray("random",
                "double",
                DataType.Double,
                new NDSize(new int[]{5 * 5}));
    }

    @After
    public void tearDown() {
        String location = file.getLocation();

        file.close();

        // delete file
        java.io.File f = new java.io.File(location);
        f.delete();
    }

    @Test
    public void testValidate() {
        // dims are not equal data dims: 1 warning
        Result result = Validator.validate(array1);
        assertTrue(result.getErrors().size() == 1);
        assertTrue(result.getWarnings().size() == 0);
    }

    @Test
    public void testId() {
        assertEquals(array1.getId().length(), 36);
    }

    @Test
    public void testName() {
        assertEquals(array1.getName(), "array_one");
    }

    @Test
    public void testType() {
        assertEquals(array1.getType(), "testdata");
    }

    @Test
    public void testDefinition() {
        assertNull(array1.getDefinition());
    }

    @Test
    public void testLabel() {
        String testStr = "somestring";

        array1.setLabel(testStr);
        assertEquals(array1.getLabel(), testStr);
        array1.setLabel(null);
        assertNull(array1.getLabel());
    }

    @Test
    public void testUnit() {
        String testStr = "somestring";
        String validUnit = "mV^2";

        try {
            array1.setUnit(testStr);
            fail();
        } catch (RuntimeException re) {
        }

        try {
            array1.setUnit(validUnit);
        } catch (Exception e) {
            fail();
        }

        assertEquals(array1.getUnit(), validUnit);

        try {
            array1.setUnit(null);
        } catch (Exception e) {
            fail();
        }

        assertNull(array1.getUnit());
    }

    @Test
    public void testData() {

        double[] A = new double[3 * 4 * 2];

        int values = 0;
        for (int i = 0; i != 3; ++i)
            for (int j = 0; j != 4; ++j)
                for (int k = 0; k != 2; ++k)
                    A[i * 4 * 2 + j * 2 + k] = values++;

        assertEquals(array1.getDataType(), DataType.Double);

        assertNull(array1.getDimension(1));

        array1.setData(A, new NDSize(new int[]{3 * 4 * 2}), new NDSize());

        double[] B = new double[3 * 4 * 2];
        array1.getData(B, new NDSize(new int[]{3 * 4 * 2}), new NDSize());

        int verify = 0;
        int errors = 0;
        for (int i = 0; i != 3; ++i) {
            for (int j = 0; j != 4; ++j) {
                for (int k = 0; k != 2; ++k) {
                    int v = verify++;
                    if (B[i * 4 * 2 + j * 2 + k] != v) {
                        errors += 1;
                    }
                }
            }
        }
        assertEquals(errors, 0);

        double[] D = new double[5 * 5];
        for (int i = 0; i != 5; ++i)
            for (int j = 0; j != 5; ++j)
                D[i * 5 + j] = 42.0;

        array2.setData(D, new NDSize(new int[]{5 * 5}), new NDSize());

        double[] E = new double[5 * 5];
        array2.getData(E, new NDSize(new int[]{5 * 5}), new NDSize());

        for (int i = 0; i != 5; ++i)
            for (int j = 0; j != 5; ++j)
                assertTrue(D[i * 5 + j] == E[i * 5 + j]);

        DataArray da3 = block.createDataArray("direct-vector",
                "double",
                DataType.Double,
                new NDSize(new int[]{5}));

        double[] dv = {1.0, 2.0, 3.0, 4.0, 5.0};
        da3.setData(dv, new NDSize(new int[]{5}), new NDSize());

        double[] dvin = new double[5];
        da3.getData(dvin, new NDSize(new int[]{5}), new NDSize());

        for (int i = 0; i < 5; i++) {
            assertTrue(dv[i] == dvin[i]);
        }
    }

    @Test
    public void testDimension() {
        double[] ticks = new double[5];
        double samplingInterval = Math.PI;

        for (int i = 0; i < 5; i++) {
            ticks[i] = Math.PI;
        }

        try {
            array2.appendRangeDimension(new double[]{});
            fail();
        } catch (RuntimeException re) {
        }

        try {
            array2.createRangeDimension(1, new double[]{});
            fail();
        } catch (RuntimeException re) {
        }

        array2.createSampledDimension(1, samplingInterval);
        array2.createSetDimension(2);
        array2.createRangeDimension(3, ticks);
        array2.appendSampledDimension(samplingInterval);
        array2.appendSetDimension();
        array2.createRangeDimension(4, ticks);

        // have some explicit dimension types
        RangeDimension dim_range = array1.appendRangeDimension(ticks);
        SampledDimension dim_sampled = array1.appendSampledDimension(samplingInterval);
        SetDimension dim_set = array1.appendSetDimension();

        assertTrue(array2.getDimensionCount() == 5);

        // since deleteDimension renumbers indices to be continuous we test that too
        array2.deleteDimension(5);
        array2.deleteDimension(4);
        array2.deleteDimension(1);
        array2.deleteDimension(1);
        array2.deleteDimension(1);

        assertTrue(array2.getDimensionCount() == 0);
    }

    @Test
    public void testCreatedAt() {
        assertTrue(array1.getCreatedAt().compareTo(statup_time) >= 0);
        assertTrue(array2.getCreatedAt().compareTo(statup_time) >= 0);

        long time = System.currentTimeMillis() - 10000000L * 1000;
        // precision of time_t is in seconds hence (millis / 1000) * 1000
        time = time / 1000 * 1000;

        Date past_time = new Date(time);
        array1.forceCreatedAt(past_time);
        array2.forceCreatedAt(past_time);
        assertTrue(array1.getCreatedAt().equals(past_time));
        assertTrue(array2.getCreatedAt().equals(past_time));
    }

    @Test
    public void testUpdatedAt() {
        assertTrue(array1.getUpdatedAt().compareTo(statup_time) >= 0);
        assertTrue(array2.getUpdatedAt().compareTo(statup_time) >= 0);
    }
}