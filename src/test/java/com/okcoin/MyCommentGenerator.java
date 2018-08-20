/**
 *
 */
package com.okcoin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.internal.DefaultCommentGenerator;

/**
 * @author changyong.cai
 */
public class MyCommentGenerator extends DefaultCommentGenerator {

    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable) {

    }

    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable,
                                IntrospectedColumn introspectedColumn) {
        String remark = introspectedColumn.getRemarks();
        if (remark != null && !"".equals( remark.trim() )) {
            String[] a = remark.split( "\n" );
            String s = remark;
            if (a.length > 1) {
                s = "";
                for (int i = 0; i < a.length; i++) {
                    s += a[i].trim() + "; ";
                }
            }
            field.addJavaDocLine( "/**" );
            field.addJavaDocLine( " * " + s );
            field.addJavaDocLine( " */" ); //$NON-NLS-1$
        }
    }

    @Override
    public void addGeneralMethodComment(Method method, IntrospectedTable introspectedTable) {
    }

    @Override
    public void addSetterComment(Method method, IntrospectedTable introspectedTable,
                                 IntrospectedColumn introspectedColumn) {
    }

    @Override
    public void addGetterComment(Method method, IntrospectedTable introspectedTable,
                                 IntrospectedColumn introspectedColumn) {
    }

    @Override
    public void addEnumComment(InnerEnum innerEnum, IntrospectedTable introspectedTable) {
    }

    @Override
    public void addComment(XmlElement xmlElement) {
    }

}
