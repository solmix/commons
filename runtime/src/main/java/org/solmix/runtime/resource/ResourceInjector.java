/**
 * Copyright 2013 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */
package org.solmix.runtime.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.commons.util.Reflection;
import org.solmix.runtime.Container;
import org.solmix.runtime.ContainerAware;
import org.solmix.runtime.ProductionAware;
import org.solmix.runtime.annotation.AbstractAnnotationVisitor;
import org.solmix.runtime.annotation.AnnotationProcessor;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2014年7月27日
 */

public class ResourceInjector extends AbstractAnnotationVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceInjector.class);
    private static final List<Class<? extends Annotation>> ANNOTATIONS = 
        new ArrayList<Class<? extends Annotation>>();
    
    static {
        ANNOTATIONS.add(Resource.class);
        ANNOTATIONS.add(Resources.class);
    }
    
    
    private final ResourceManager resourceManager; 
    private final List<ResourceResolver> resourceResolvers;

    public ResourceInjector(ResourceManager resMgr) {
        this(resMgr, resMgr == null ? null : resMgr.getResourceResolvers());
    }

    public ResourceInjector(ResourceManager resMgr, List<ResourceResolver> resolvers) {
        super(ANNOTATIONS);
        resourceManager = resMgr;
        resourceResolvers = resolvers;
    }
    
    private static Field getField(Class<?> cls, String name) {
        if (cls == null) {
            return null;
        }
        try {
            return cls.getDeclaredField(name);
        } catch (Exception ex) {
            return getField(cls.getSuperclass(), name);
        }
    }
    
    public static boolean processable(Class<?> cls, Object o) {
        if (cls.getName().startsWith("java.")
            || cls.getName().startsWith("javax.")) {
            return false;
        }
        return true;
    }
    
    public void inject(Object o) {        
        inject(o, o.getClass());
    }
    
    public void injectAware(Object o){
        Container container=null;
        if(ContainerAware.class.isInstance(o)){
            container=(Container)resolveResource(null, Container.class);
            ContainerAware.class.cast(o).setContainer(container);
        }
        if(ProductionAware.class.isInstance(o)){
            if(container==null){
                container=(Container)resolveResource(null, Container.class);
            }
            ProductionAware.class.cast(o).setProduction(container.isProduction());
        }
    }
    
    
    public void inject(Object o, Class<?> claz) {
        if (processable(claz, o)) {
            AnnotationProcessor processor = new AnnotationProcessor(o); 
            processor.accept(this, claz);
        }
    }
    
    public void construct(Object o) {
        setTarget(o);
        if (processable(targetClass, o)) {
            invokePostConstruct();
        }
    }
    public void construct(Object o, Class<?> cls) {
        setTarget(o, cls);
        if (processable(targetClass, o)) {
            invokePostConstruct();
        }
    }


    public void destroy(Object o) {
        setTarget(o);
        if (processable(targetClass, o)) {
            invokePreDestroy();
        }
    }



    @Override
    public final void visitClass(final Class<?> clz, final Annotation annotation) {
        
        assert annotation instanceof Resource || annotation instanceof Resources : annotation; 

        if (annotation instanceof Resource) { 
            injectResourceClassLevel(clz, (Resource)annotation); 
        } else if (annotation instanceof Resources) { 
            Resources resources = (Resources)annotation;
            for (Resource resource : resources.value()) {
                injectResourceClassLevel(clz, resource); 
            }
        } 

    }

    private void injectResourceClassLevel(Class<?> clz, Resource res) { 
        if (res.name() == null || "".equals(res.name())) { 
            LOG.info( "Resource name not specified for type:{}", target.getClass().getName());
            return;
        } 

        Object resource = null;
        // first find a setter that matches this resource
        Method setter = findSetterForResource(res);
        if (setter != null) { 
            Class<?> type = getResourceType(res, setter); 
            resource = resolveResource(res.name(), type);
            if (resource == null) {
                LOG.info(  "RESOURCE_RESOLVE_FAILED");
                return;
            } 

            invokeSetter(setter, resource);
            return;
        }
        
        Field field = findFieldForResource(res);
        if (field != null) { 
            Class<?> type = getResourceType(res, field); 
            resource = resolveResource(res.name(), type);
            if (resource == null) {
                LOG.info( "RESOURCE_RESOLVE_FAILED");
                return;
            } 
            injectField(field, resource); 
            return;
        }
        LOG.error( "NO_SETTER_OR_FIELD_FOR_RESOURCE", getTarget().getClass().getName());
    } 

    @Override
    public final void visitField(final Field field, final Annotation annotation) {

        assert annotation instanceof Resource : annotation;
        
        Resource res = (Resource)annotation;

        String name = getFieldNameForResource(res, field);
        Class<?> type = getResourceType(res, field); 
        
        Object resource = resolveResource(name, type);
        if (resource == null
            && "".equals(res.name())) {
            resource = resolveResource(null, type);
        }
        if (resource != null) {
            injectField(field, resource);
        } else {
            LOG.warn("Resolve resource failed, resource name: {}", name);
        }
    }

    @Override
    public final void visitMethod(final Method method, final Annotation annotation) {
        
        assert annotation instanceof Resource : annotation;

        Resource res = (Resource)annotation; 
        
        String resourceName = getResourceName(res, method);
        Class<?> clz = getResourceType(res, method); 

        Object resource = resolveResource(resourceName, clz);
        if (resource == null && "".equals(res.name())) {
            resource = resolveResource(null, clz);
        }
        if (resource != null) {
            invokeSetter(method, resource);
        } else {
            LOG.warn("Resolve resource failed, resource name:{},type:{} ", new Object[] {resourceName,clz });
        }
    }

    private Field findFieldForResource(Resource res) {
        assert target != null; 
        assert res.name() != null;

        for (Field field : target.getClass().getFields()) { 
            if (field.getName().equals(res.name())) { 
                return field;
            } 
        }

        for (Field field : target.getClass().getDeclaredFields()) { 
            if (field.getName().equals(res.name())) { 
                return field;
            } 
        }
        return null;
    }


    private Method findSetterForResource(Resource res) {
        assert target != null; 

        String setterName = resourceNameToSetter(res.name());
        Method setterMethod = null;

        for (Method method : getTarget().getClass().getMethods()) {
            if (setterName.equals(method.getName())) {
                setterMethod = method;
                break;
            }
        }
        
        if (setterMethod != null && setterMethod.getParameterTypes().length != 1) {
            LOG.warn("SETTER_INJECTION_WITH_INCORRECT_TYPE", setterMethod);
        }
        return setterMethod;
    }

    
    private String resourceNameToSetter(String resName) {

        return "set" + Character.toUpperCase(resName.charAt(0)) + resName.substring(1);
    }
    

    private void invokeSetter(Method method, Object resource) { 
        try {
            Reflection.setAccessible(method);
            if (method.getDeclaringClass().isAssignableFrom(getTarget().getClass())) {
                method.invoke(getTarget(), resource);
            } else { // deal with the proxy setter method
                Method targetMethod = getTarget().getClass().getMethod(method.getName(),
                                                                       method.getParameterTypes()); 
                targetMethod.invoke(getTarget(), resource);
            }
        } catch (IllegalAccessException e) { 
            LOG.error( "INJECTION_SETTER_NOT_VISIBLE", method);
        } catch (InvocationTargetException e) { 
            LOG.error("INJECTION_SETTER_RAISED_EXCEPTION", e, method);
        } catch (SecurityException e) {
            LOG.error("INJECTION_SETTER_RAISED_EXCEPTION", e, method);
        } catch (NoSuchMethodException e) {
            LOG.error( "INJECTION_SETTER_METHOD_NOT_FOUND", new Object[] {method.getName()});
        } 
    } 

    private String getResourceName(Resource res, Method method) { 
        assert method != null; 
        assert res != null; 
        assert method.getName().startsWith("set") : method;

        if (res.name() == null || "".equals(res.name())) {
            String name = method.getName().substring(3);
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            return new StringBuilder().append(
                method.getDeclaringClass().getCanonicalName()).append("/")
                .append(name).toString();
        }
        return res.name();
    } 



    private void injectField(Field field, Object resource) { 
        assert field != null; 
        assert resource != null; 

        boolean accessible = field.isAccessible(); 
        try {
            if (field.getType().isAssignableFrom(resource.getClass())) {
                Reflection.setAccessible(field);
//                field.set(ClassHelper.getRealObject(getTarget()), resource);
                field.set(getTarget(), resource);
            }
        } catch (Exception e) { 
            LOG.error("Failed to inject field.",e); 
        } finally {
            Reflection.setAccessible(field, accessible);
        }
    } 


    public void invokePostConstruct() {
        
        boolean accessible = false; 
        for (Method method : getPostConstructMethods()) {
            PostConstruct pc =findAnnotation(method,PostConstruct.class);
            if (pc != null) {
                try {
                    Reflection.setAccessible(method);
                    method.invoke(target);
                } catch (IllegalAccessException e) {
                    LOG.warn( "inject method {} is not visible", method);
                } catch (InvocationTargetException e) {
                    LOG.warn("injection throw exception", e);
                } finally {
                    Reflection.setAccessible(method, accessible);
                }
            }
        }
    }

    public void invokePreDestroy() {
        
        boolean accessible = false; 
        for (Method method : getPreDestroyMethods()) {
            PreDestroy pd =findAnnotation(method,PreDestroy.class);
            if (pd != null) {
                try {
                    Reflection.setAccessible(method);
                    method.invoke(target);
                } catch (IllegalAccessException e) {
                    LOG.warn( "Pre destroy method:{} is not visible", method);
                } catch (InvocationTargetException e) {
                    LOG.warn("Pre destroy throw exception", e);
                } finally {
                    Reflection.setAccessible(method, accessible);
                }
            }
        }
    }


    private Collection<Method> getPostConstructMethods() { 
        return getAnnotatedMethods(PostConstruct.class);
    }

    private Collection<Method> getPreDestroyMethods() { 
        return getAnnotatedMethods(PreDestroy.class);
    }

    private Collection<Method> getAnnotatedMethods(Class<? extends Annotation> acls) { 
    	
        Collection<Method> methods = new LinkedList<Method>(); 
        addAnnotatedMethods(acls, getTarget().getClass().getMethods(), methods); 
        addAnnotatedMethods(acls, Reflection.getDeclaredMethods(getTarget().getClass()), methods);
        
        if (getTargetClass() != getTarget().getClass()) {
            addAnnotatedMethods(acls, getTargetClass().getMethods(), methods); 
            addAnnotatedMethods(acls, Reflection.getDeclaredMethods(getTargetClass()), methods);            
        }
        return methods;
    }
    private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>[] ifcs) {
		A annotation = null;
		for (Class<?> iface : ifcs) {
				try {
					Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
					annotation = equivalentMethod.getAnnotation(annotationType);
				}
				catch (NoSuchMethodException ex) {
					// Skip this interface - it doesn't have the method...
				}
				if (annotation != null) {
					break;
				}
		}
		return annotation;
	}
    public static <A extends Annotation> A  findAnnotation(Method method, Class<A> acls) {

    	A  annotation =method.getAnnotation(acls);
    	Class<?> clazz = method.getDeclaringClass();
    	if (annotation == null) {
			annotation = searchOnInterfaces(method, acls, clazz.getInterfaces());
		}
    	while (annotation == null) {
			clazz = clazz.getSuperclass();
			if (clazz == null || clazz.equals(Object.class)) {
				break;
			}
			try {
				Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
				annotation = equivalentMethod.getAnnotation(acls);
			}
			catch (NoSuchMethodException ex) {
				// No equivalent method found
			}
			if (annotation == null) {
				annotation = searchOnInterfaces(method, acls, clazz.getInterfaces());
			}
		}
    	return annotation;
    }
    private void addAnnotatedMethods(Class<? extends Annotation> acls, Method[] methods,
        Collection<Method> annotatedMethods) {
        for (Method method : methods) {
        	Annotation  annotation =findAnnotation(method, acls);
        	
            if (annotation != null   && !annotatedMethods.contains(method)) {
                annotatedMethods.add(method); 
            }
        }
    } 
     
        
    /**
     * making this protected to keep pmd happy
     */
    protected Class<?> getResourceType(Resource res, Field field) {
        assert res != null;
        Class<?> type = res.type();
        if (res.type() == null || Object.class == res.type()) {
            type = field.getType();
        }
        return type;
    }


    private Class<?> getResourceType(Resource res, Method method) { 
        return res.type() != null && !Object.class.equals(res.type()) 
            ? res.type() 
            : method.getParameterTypes()[0];
    } 


    private String getFieldNameForResource(Resource res, Field field) {
        assert res != null;
        if (res.name() == null || "".equals(res.name())) {
            return field.getDeclaringClass().getCanonicalName() + "/" + field.getName();
        }
        return res.name();
    }

    private Object resolveResource(String resourceName, Class<?> type) {
        if (resourceManager == null) {
            return null;
        }
        return resourceManager.resolveResource(resourceName, type, resourceResolvers);
    }

}
