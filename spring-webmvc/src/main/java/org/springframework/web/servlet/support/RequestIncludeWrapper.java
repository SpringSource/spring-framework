/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request wrapper to be used for server side includes.
 * Contains its own custom parameter map which may be supplied to the constructor.
 * It may optionally fall back to the original request parameters.By default fall back is disabled. If it is enabled, all request parameters
 * are merged with the custom parameters.A custom parameter for a given parameter name overrides the request parameter with the same name.
 *
 * @Author Cagatay Kalan
 * @Since 4.3.0
 *
 *
 */
public class RequestIncludeWrapper extends HttpServletRequestWrapper
{

    private Map<String,String[]> parameterMap;
    private boolean fallBackParameters = false;

    public RequestIncludeWrapper(HttpServletRequest request)
    {

        this(request,null);
    }

    public RequestIncludeWrapper(HttpServletRequest request, Map<String, String[]> parameterMap)
    {
        this(request,parameterMap,false);
    }

    /**
     * @param request
     * Wrapped request
     * @param fallBackToOriginalRequestParameters
     * Determines whether original request parameter map is merged with the custom parameters set on this instance.
     */
    public RequestIncludeWrapper(HttpServletRequest request, boolean fallBackToOriginalRequestParameters)
    {
        this(request,null, fallBackToOriginalRequestParameters);
    }

    /**
     * @param request
     * Wrapped request
     * @param parameterMap
     * Custom parameter map.Overrides original request parameters.
     * @param fallBackToOriginalRequestParameters
     * Determines whether original request parameter map is merged with the custom parameters set on this instance.
     */
    public RequestIncludeWrapper(HttpServletRequest request,Map<String,String[]> parameterMap, boolean fallBackToOriginalRequestParameters)
    {
        super(request);
        this.parameterMap = parameterMap;
        this.fallBackParameters = fallBackToOriginalRequestParameters;
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        return ensureParameterMap();
    }


    @Override
    public String[] getParameterValues(String name)
    {
        return ensureParameterMap().get(name);
    }

    @Override
    public String getParameter(String name)
    {
        String[] values = getParameterValues(name);
        if (values != null && values.length > 0)
            return values[0];

        return null;
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(parameterMap.keySet());
    }

    protected Map<String,String[]> ensureParameterMap()
    {
        Map<String,String[]> givenParameters = parameterMap;
        parameterMap = new LinkedHashMap<String, String[]>();
        if (fallBackParameters)
            parameterMap.putAll(getRequest().getParameterMap());

        if (givenParameters != null)
            parameterMap.putAll(givenParameters);

        parameterMap = Collections.unmodifiableMap(parameterMap);
        return parameterMap;

    }


}
