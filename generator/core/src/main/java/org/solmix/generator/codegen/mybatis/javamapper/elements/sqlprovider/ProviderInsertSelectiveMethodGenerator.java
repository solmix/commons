/**
 *    Copyright 2006-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.solmix.generator.codegen.mybatis.javamapper.elements.sqlprovider;

import static org.solmix.generator.codegen.mybatis.MyBatis3FormattingUtilities.getEscapedColumnName;
import static org.solmix.generator.codegen.mybatis.MyBatis3FormattingUtilities.getParameterClause;
import static org.solmix.generator.internal.util.JavaBeansUtil.getGetterMethodName;
import static org.solmix.generator.internal.util.StringUtility.escapeStringForJava;

import java.util.Set;
import java.util.TreeSet;

import org.solmix.generator.api.IntrospectedColumn;
import org.solmix.generator.api.java.FullyQualifiedJavaType;
import org.solmix.generator.api.java.JavaVisibility;
import org.solmix.generator.api.java.Method;
import org.solmix.generator.api.java.Parameter;
import org.solmix.generator.api.java.TopLevelClass;
import org.solmix.generator.codegen.mybatis.ListUtilities;

/**
 * 
 * @author Jeff Butler
 * 
 */
public class ProviderInsertSelectiveMethodGenerator extends AbstractJavaProviderMethodGenerator {

    public ProviderInsertSelectiveMethodGenerator(boolean useLegacyBuilder) {
        super(useLegacyBuilder);
    }

    @Override
    public void addClassElements(TopLevelClass topLevelClass) {
        Set<String> staticImports = new TreeSet<String>();
        Set<FullyQualifiedJavaType> importedTypes = new TreeSet<FullyQualifiedJavaType>();

        if (useLegacyBuilder) {
            staticImports.add("org.apache.ibatis.jdbc.SqlBuilder.BEGIN"); //$NON-NLS-1$
            staticImports.add("org.apache.ibatis.jdbc.SqlBuilder.INSERT_INTO"); //$NON-NLS-1$
            staticImports.add("org.apache.ibatis.jdbc.SqlBuilder.SQL"); //$NON-NLS-1$
            staticImports.add("org.apache.ibatis.jdbc.SqlBuilder.VALUES"); //$NON-NLS-1$
        } else {
            importedTypes.add(NEW_BUILDER_IMPORT);
        }

        FullyQualifiedJavaType fqjt = introspectedTable.getRules()
                .calculateAllFieldsClass();
        importedTypes.add(fqjt);

        Method method = new Method(
                introspectedTable.getInsertSelectiveStatementId());
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(FullyQualifiedJavaType.getStringInstance());
        method.addParameter(new Parameter(fqjt, "record")); //$NON-NLS-1$
        
        context.getCommentGenerator().addGeneralMethodComment(method,
                introspectedTable);

        if (useLegacyBuilder) {
            method.addBodyLine("BEGIN();"); //$NON-NLS-1$
        } else {
            method.addBodyLine("SQL sql = new SQL();"); //$NON-NLS-1$
        }

        method.addBodyLine(String.format("%sINSERT_INTO(\"%s\");", //$NON-NLS-1$
                builderPrefix,
                escapeStringForJava(introspectedTable.getFullyQualifiedTableNameAtRuntime())));

        for (IntrospectedColumn introspectedColumn : ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns())) {
            
            method.addBodyLine(""); //$NON-NLS-1$
            if (!introspectedColumn.getFullyQualifiedJavaType().isPrimitive()
                    && !introspectedColumn.isSequenceColumn()) {
                method.addBodyLine(String.format("if (record.%s() != null) {", //$NON-NLS-1$
                        getGetterMethodName(introspectedColumn.getJavaProperty(),
                                introspectedColumn.getFullyQualifiedJavaType())));
            }
            method.addBodyLine(String.format("%sVALUES(\"%s\", \"%s\");", //$NON-NLS-1$
                    builderPrefix,
                    escapeStringForJava(getEscapedColumnName(introspectedColumn)),
                    getParameterClause(introspectedColumn)));

            if (!introspectedColumn.getFullyQualifiedJavaType().isPrimitive()
                    && !introspectedColumn.isSequenceColumn()) {
                method.addBodyLine("}"); //$NON-NLS-1$
            }
        }

        method.addBodyLine(""); //$NON-NLS-1$
        if (useLegacyBuilder) {
            method.addBodyLine("return SQL();"); //$NON-NLS-1$
        } else {
            method.addBodyLine("return sql.toString();"); //$NON-NLS-1$
        }
        
        if (context.getPlugins().providerInsertSelectiveMethodGenerated(method, topLevelClass,
                introspectedTable)) {
            topLevelClass.addStaticImports(staticImports);
            topLevelClass.addImportedTypes(importedTypes);
            topLevelClass.addMethod(method);
        }
    }
}