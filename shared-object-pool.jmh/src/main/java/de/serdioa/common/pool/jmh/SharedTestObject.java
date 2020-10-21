package de.serdioa.common.pool.jmh;

import de.serdioa.common.pool.SharedObject;


/**
 * A shared wrapper for a test object provided by an object pool.
 */
public interface SharedTestObject extends TestObject, SharedObject {

}
