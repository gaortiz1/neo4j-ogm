/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.persistence.relationships.transitive.abb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.persistence.relationships.direct.RelationshipTrait;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class ABBTest extends RelationshipTrait {

    private static SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.persistence.relationships.transitive.abb");
    private Session session;
    private A a;
    private B b1, b2;
    private R r1, r2;

    @Before
    public void init() throws IOException {
        session = sessionFactory.openSession();
        setUpEntityModel();
    }
    @After
    public void cleanup() {
        session.purgeDatabase();
    }

    private void setUpEntityModel() {

        a = new A();

        b1 = new B();
        b2 = new B();

        r1 = new R();
        r2 = new R();

        r1.a = a;
        r1.b = b1;

        r2.a = a;
        r2.b = b2;

        a.r = new R[]{r1, r2};
        b1.r = r1;
        b2.r = r2;

    }

    @Test
    public void shouldFindBFromA() {

        session.save(b1);


        a = session.load(A.class, a.id);

        assertEquals(2, a.r.length);
        assertSameArray(new B[]{a.r[0].b, a.r[1].b}, new B[]{b1, b2});

    }

    @Test
    public void shouldFindAFromB() {

        session.save(a);

        b1 = session.load(B.class, b1.id);
        b2 = session.load(B.class, b2.id);

        assertEquals(a, b1.r.a);
        assertEquals(a, b2.r.a);

    }

    @Test
    public void shouldReflectRemovalA() {

        session.save(a);

        // local model must be self-consistent
        b1.r = null;
        a.r = new R[]{r2};

        session.save(b1);

        // when we reload a
        a = session.load(A.class, a.id);

        // expect the b1 relationship to have gone.
        assertEquals(1, a.r.length);
        assertSameArray(new B[]{b2}, new B[]{a.r[0].b});

    }

    @Test
    public void shouldBeAbleToAddNewB() {

        session.save(a);

        B b3 = new B();
        R r3 = new R();

        r3.a = a;
        r3.b = b3;
        b3.r = r3;
        a.r = new R[]{r1, r2, r3};

        // fully connected graph, should be able to save any object
        session.save(a);

        // try others?

        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[]{a}, new A[]{b3.r.a});

    }

    @Test
    public void shouldBeAbleToAddNewR() {

        session.save(a);

        B b3 = new B();
        R r3 = new R();

        r3.a = a;
        r3.b = b3;
        b3.r = r3;
        a.r = new R[]{r1, r2, r3};

        // fully connected graph, should be able to save any object
        session.save(r3);

        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[]{a}, new A[]{b3.r.a});
        assertSameArray(new R[]{r1, r2, r3}, a.r);
        assertSameArray(new B[]{b1, b2, b3}, new B[]{a.r[0].b, a.r[1].b, a.r[2].b});

    }

    /**
     * @see DATAGRAPH-714
     */
    @Test
    public void shouldBeAbleToUpdateRBySavingA() {
        A a1 = new A();
        B b3 = new B();
        R r3 = new R();
        r3.a = a1;
        r3.b = b3;
        r3.number = 1;
        a1.r = new R[]{r3};
        b3.r = r3;

        session.save(a1);
        r3.number = 2;
        session.save(a1);

        session.clear();
        b3 = session.load(B.class, b3.id);
        assertEquals(2, b3.r.number);
    }

    /**
     * @see DATAGRAPH-714
     */
    @Test
    public void shouldBeAbleToUpdateRBySavingB() {
        A a1 = new A();
        B b3 = new B();
        R r3 = new R();
        r3.a = a1;
        r3.b = b3;
        r3.number = 1;
        a1.r = new R[]{r3};
        b3.r = r3;

        session.save(a1);
        r3.number = 2;
        session.save(b3);

        session.clear();
        b3 = session.load(B.class, b3.id);
        assertEquals(2, b3.r.number);
    }

    /**
     * @see DATAGRAPH-714
     */
    @Test
    public void shouldBeAbleToUpdateRBySavingR() {
        A a1 = new A();
        B b3 = new B();
        R r3 = new R();
        r3.a = a1;
        r3.b = b3;
        r3.number = 1;
        a1.r = new R[]{r3};
        b3.r = r3;

        session.save(a1);
        r3.number = 2;
        session.save(r3);

        session.clear();
        b3 = session.load(B.class, b3.id);
        assertEquals(2, b3.r.number);
    }

    @NodeEntity(label = "A")
    public static class A extends E {
        @Relationship(type = "EDGE", direction = Relationship.OUTGOING)
        R[] r;
    }

    @NodeEntity(label = "B")
    public static class B extends E {
        @Relationship(type = "EDGE", direction = Relationship.INCOMING)
        R r;
    }

    /**
     * Can be used as the basic class at the root of any entity for these tests,
     * provides the mandatory id field, a unique ref, a simple to-string method
     * and equals/hashcode implementation.
     * <p/>
     * Note that without an equals/hashcode implementation, reloading
     * an object which already has a collection of items in it
     * will result in the collection items being added again, because
     * of the behaviour of the ogm merge function when handling
     * arrays and iterables.
     */
    public abstract static class E {

        public Long id;
        public String key;

        public E() {
            this.key = UUID.randomUUID().toString();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + id + ":" + key;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            return (key.equals(((E) o).key));
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }


    @RelationshipEntity(type = "EDGE")
    public static class R {

        Long id;

        @StartNode
        A a;
        @EndNode
        B b;

        int number;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + a.id + "->" + b.id;
        }

    }

}
