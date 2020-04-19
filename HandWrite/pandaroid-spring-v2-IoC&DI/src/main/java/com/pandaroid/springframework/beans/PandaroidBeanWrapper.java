package com.pandaroid.springframework.beans;

/**
 * @author pandaroid
 */
public class PandaroidBeanWrapper {

    private Object wrappedInstance;
    private Class<?> wrappedClass;
    public PandaroidBeanWrapper(Object instance) {
        wrappedInstance = instance;
        wrappedClass = instance.getClass();
    }

    public Object getWrappedInstance() {
        return wrappedInstance;
    }

    public void setWrappedInstance(Object wrappedInstance) {
        this.wrappedInstance = wrappedInstance;
    }

    public Class<?> getWrappedClass() {
        return wrappedClass;
    }

    public void setWrappedClass(Class<?> wrappedClass) {
        this.wrappedClass = wrappedClass;
    }

}
