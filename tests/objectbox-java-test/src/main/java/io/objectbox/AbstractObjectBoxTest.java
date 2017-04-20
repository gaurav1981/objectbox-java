package io.objectbox;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.ModelBuilder.PropertyBuilder;
import io.objectbox.internal.IdGetter;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractObjectBoxTest {
    protected File boxStoreDir;
    protected BoxStore store;
    protected Random random = new Random();
    protected boolean runExtensiveTests;

    int lastEntityId;
    int lastIndexId;
    long lastUid;
    long lastEntityUid;
    long lastIndexUid;

    @Before
    public void setUp() throws IOException {
        // This works with Android without needing any context
        File tempFile = File.createTempFile("object-store-test", "");
        tempFile.delete();
        boxStoreDir = tempFile;
        store = createBoxStore();
        runExtensiveTests = System.getProperty("extensive") != null;
    }

    protected BoxStore createBoxStore() {
        return createBoxStore(false);
    }

    protected BoxStore createBoxStore(boolean withIndex) {
        return createBoxStoreBuilder(withIndex).build();
    }

    protected BoxStoreBuilder createBoxStoreBuilderWithTwoEntities(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModelWithTwoEntities(withIndex)).directory(boxStoreDir);
        builder.entity("TestEntity", TestEntity.class, TestEntityCursor.class, new TestEntity_());
        builder.entity("TestEntityMinimal", TestEntityMinimal.class, TestEntityMinimalCursor.class, new Properties() {
            @Override
            public Property[] getAllProperties() {
                return new Property[0];
            }

            @Override
            public Property getIdProperty() {
                return null;
            }

            @Override
            public String getDbName() {
                return null;
            }

            @Override
            public IdGetter getIdGetter() {
                return new IdGetter<TestEntityMinimal>() {
                    @Override
                    public long getId(TestEntityMinimal object) {
                        return object.getId();
                    }
                };
            }
        });
        return builder;
    }

    protected BoxStoreBuilder createBoxStoreBuilder(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(withIndex)).directory(boxStoreDir);
        builder.entity("TestEntity", TestEntity.class, TestEntityCursor.class, new TestEntity_());
        return builder;
    }

    protected Box<TestEntity> getTestEntityBox() {
        return store.boxFor(TestEntity.class);
    }

    @After
    public void tearDown() throws Exception {
        // Collect dangling Cursors and TXs before store closes
        System.gc();
        System.runFinalization();

        if (store != null) {
            try {
                store.close();
                store.deleteAllFiles();

                File[] files = boxStoreDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        logError("File was not deleted: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logError("Could not clean up test", e);
            }
        }
        deleteAllFiles();
    }

    protected void deleteAllFiles() {
        if (boxStoreDir != null && boxStoreDir.exists()) {
            File[] files = boxStoreDir.listFiles();
            for (File file : files) {
                delete(file);
            }
            delete(boxStoreDir);
        }
    }

    private boolean delete(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            file.deleteOnExit();
            logError("Could not delete " + file.getAbsolutePath());
        }
        return deleted;
    }

    protected void log(String text) {
        System.out.println(text);
    }

    protected void logError(String text) {
        System.err.println(text);
    }

    protected void logError(String text, Exception ex) {
        if (text != null) {
            System.err.println(text);
        }
        ex.printStackTrace();
    }

    protected long time() {
        return System.currentTimeMillis();
    }

    byte[] createTestModel(boolean withIndex) {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTestEntity(modelBuilder, withIndex);
        modelBuilder.lastEntityId(lastEntityId, lastEntityUid);
        modelBuilder.lastIndexId(lastIndexId, lastIndexUid);
        return modelBuilder.build();
    }

    byte[] createTestModelWithTwoEntities(boolean withIndex) {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTestEntity(modelBuilder, withIndex);
        addTestEntityMinimal(modelBuilder, withIndex);
        modelBuilder.lastEntityId(lastEntityId, lastEntityUid);
        modelBuilder.lastIndexId(lastIndexId, lastIndexUid);
        return modelBuilder.build();
    }

    private void addTestEntity(ModelBuilder modelBuilder, boolean withIndex) {
        lastEntityUid = ++lastUid;
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntity").id(++lastEntityId, lastEntityUid);
        entityBuilder.property("id", PropertyType.Long).id(TestEntity_.id.id, ++lastUid)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE);
        entityBuilder.property("simpleBoolean", PropertyType.Bool).id(TestEntity_.simpleBoolean.id, ++lastUid);
        entityBuilder.property("simpleByte", PropertyType.Byte).id(TestEntity_.simpleByte.id, ++lastUid);
        entityBuilder.property("simpleShort", PropertyType.Short).id(TestEntity_.simpleShort.id, ++lastUid);
        entityBuilder.property("simpleInt", PropertyType.Int).id(TestEntity_.simpleInt.id, ++lastUid);
        entityBuilder.property("simpleLong", PropertyType.Long).id(TestEntity_.simpleLong.id, ++lastUid);
        entityBuilder.property("simpleFloat", PropertyType.Float).id(TestEntity_.simpleFloat.id, ++lastUid);
        entityBuilder.property("simpleDouble", PropertyType.Double).id(TestEntity_.simpleDouble.id, ++lastUid);
        PropertyBuilder pb =
                entityBuilder.property("simpleString", PropertyType.String).id(TestEntity_.simpleString.id, ++lastUid);
        if (withIndex) {
            lastIndexUid = ++lastUid;
            pb.flags(PropertyFlags.INDEXED).indexId(++lastIndexId, lastIndexUid);
        }
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector).id(TestEntity_.simpleByteArray.id, ++lastUid);
        int lastId = TestEntity_.simpleByteArray.id;
        entityBuilder.lastPropertyId(lastId, lastUid);
        entityBuilder.entityDone();
    }

    private void addTestEntityMinimal(ModelBuilder modelBuilder, boolean withIndex) {
        lastEntityUid = ++lastUid;
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntityMinimal").id(++lastEntityId, lastEntityUid);
        int pId = 0;
        entityBuilder.property("id", PropertyType.Long).id(++pId, ++lastUid).flags(PropertyFlags.ID);
        long lastPropertyUid = ++lastUid;
        PropertyBuilder pb = entityBuilder.property("text", PropertyType.String).id(++pId, lastPropertyUid);
        if (withIndex) {
            lastIndexUid = ++lastUid;
            pb.flags(PropertyFlags.INDEXED).indexId(++lastIndexId, lastIndexUid);
        }
        entityBuilder.lastPropertyId(pId, lastPropertyUid);
        entityBuilder.entityDone();
    }

    protected TestEntity createTestEntity(String simpleString, int nr) {
        TestEntity entity = new TestEntity();
        entity.setSimpleString(simpleString);
        entity.setSimpleInt(nr);
        entity.setSimpleByte((byte) (10 + nr));
        entity.setSimpleBoolean(nr % 2 == 0);
        entity.setSimpleShort((short) (100 + nr));
        entity.setSimpleLong(1000 + nr);
        entity.setSimpleFloat(200 + nr / 10f);
        entity.setSimpleDouble(2000 + nr / 100f);
        return entity;
    }

    protected TestEntity putTestEntity(String simpleString, int nr) {
        TestEntity entity = createTestEntity(simpleString, nr);
        long key = getTestEntityBox().put(entity);
        assertTrue(key != 0);
        assertEquals(key, entity.getId());
        return entity;
    }

    protected List<TestEntity> putTestEntities(int count, String baseString, int baseNr) {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = baseNr; i < baseNr + count; i++) {
            entities.add(createTestEntity(baseString != null ? baseString + i : null, i));
        }
        getTestEntityBox().put(entities);
        return entities;
    }

    protected List<TestEntity> putTestEntities(int count) {
        return putTestEntities(count, "foo", 1);
    }

    protected void assertLatchCountedDown(CountDownLatch latch, int seconds) {
        try {
            assertTrue(latch.await(seconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}