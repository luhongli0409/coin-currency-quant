package com.okcoin; /**
 *
 */

import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

import java.sql.Types;

/**
 * @author changyong.cai
 */
public class JavaTypeResolverImpl extends JavaTypeResolverDefaultImpl {

    /**
     *
     */
    public JavaTypeResolverImpl() {
        super();

        // 更改TINYINT 映射为int
        typeMap.put( Types.TINYINT, new JdbcTypeInformation( "TINYINT", //$NON-NLS-1$
            new FullyQualifiedJavaType( Integer.class.getName() ) ) );
        typeMap.put( Types.SMALLINT, new JdbcTypeInformation( "SMALLINT", //$NON-NLS-1$
            new FullyQualifiedJavaType( Integer.class.getName() ) ) );

        typeMap.put( Types.BLOB, new JdbcTypeInformation( "BLOB", //$NON-NLS-1$
            new FullyQualifiedJavaType( Object.class.getName() ) ) );
    }

}
