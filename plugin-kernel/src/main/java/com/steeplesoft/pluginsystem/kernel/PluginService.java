/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU 
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You 
 * may not use this file except in compliance with the License.  You can 
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL 
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.steeplesoft.pluginsystem.kernel;

import com.steeplesoft.pluginsystem.data.ConsolePlugin;
import com.steeplesoft.pluginsystem.data.NavNodes;
import com.steeplesoft.pluginsystem.data.ViewFragment;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 *
 * @author jasonlee
 */
public class PluginService {
    @Inject
    Instance<ConsolePlugin> plugins;

    private List<ConsolePluginMetadata> metadata;
    private static final Set<String> classNames = new HashSet<String>();

    public List<ConsolePluginMetadata> getPlugins() {
//        if (metadata == null) {

            metadata = new ArrayList<ConsolePluginMetadata>();
            Iterator<ConsolePlugin> iter = plugins.iterator();
            
//            for (ConsolePlugin cp : habitat.getAllByContract(ConsolePlugin.class)) {
            while (iter.hasNext()) {
                ConsolePlugin cp = iter.next();
                ConsolePluginMetadata cpm = new ConsolePluginMetadata(cp.getPriority());
                Class clazz = cp.getClass();
                try {
                    processAnnotations(cpm, clazz);
                    cpm.setPluginPackage(clazz.getPackage().getName());
                } catch (Exception ex) {
                    Logger.getLogger(PluginService.class.getName()).log(Level.SEVERE, null, ex);
                }

                metadata.add(cpm);
            }

            Collections.sort(metadata, new ConsolePluginComparator());
//        }
        return Collections.unmodifiableList(metadata);
    }

    protected void processAnnotations(ConsolePluginMetadata cp, Class<?> clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        for (Field field : clazz.getFields()) {
            ViewFragment vf = field.getAnnotation(ViewFragment.class);
            if (vf != null) {
                cp.addViewFragment(vf.type(), (String) field.get(clazz));
            }
            NavNodes nn = field.getAnnotation(NavNodes.class);
            if (nn != null) {
                cp.addNavigationNodes(nn.parent(), (List<NavigationNode>) field.get (clazz));
            }
        }
        
        for (Method method : clazz.getMethods()) {
            NavNodes nn = method.getAnnotation(NavNodes.class);
            if (nn != null) {
                cp.addNavigationNodes(nn.parent(), (List<NavigationNode>) method.invoke (null, new Object[]{}));
            }
        }
    }

    public void addClass(String className) {
        classNames.add(className);
    }

    public static Set<String> getClassNames() {
        return classNames;
    }
}
