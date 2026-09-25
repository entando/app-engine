package org.entando.entando.aps.servlet;

import freemarker.template.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class RequestParameterValuesHashModel implements TemplateHashModelEx {

    private final HttpServletRequest request;
    private final ObjectWrapper wrapper;
    private List keys;

    public RequestParameterValuesHashModel(HttpServletRequest request, ObjectWrapper wrapper) {
        this.request = request;
        this.wrapper = wrapper;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        String[] value = this.request.getParameterValues(key);
        return value == null ? null : wrapper.wrap(request.getParameterValues(key));
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return !this.request.getParameterNames().hasMoreElements();
    }

    @Override
    public int size() {
        return this.getKeys().size();
    }

    @Override
    public TemplateCollectionModel keys() {
        return new SimpleCollection(this.getKeys().iterator());
    }

    @Override
    public TemplateCollectionModel values() {
        final Iterator iter = this.getKeys().iterator();
        return new SimpleCollection(new Iterator() {
            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return RequestParameterValuesHashModel.this.request.getParameterValues((String) iter.next());
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }

    private synchronized List getKeys() {
        if (this.keys == null) {
            this.keys = new ArrayList();
            Enumeration enumeration = this.request.getParameterNames();

            while(enumeration.hasMoreElements()) {
                this.keys.add(enumeration.nextElement());
            }
        }
        return this.keys;
    }

}
