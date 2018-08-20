/**
 *
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 生成基础代码, 注意两个配置文件: code/generatorConfig.xml, code/mybatis.onproperties
 */
public class CodeUtil {

    static Logger logger = LoggerFactory.getLogger( CodeUtil.class );

    /**
     * @param args
     */
    public static void main(String[] args) {
        //
        String generatorConfig = CodeUtil.class.getClassLoader().getResource( "code/generator.xml" ).getFile();
        CodeGenerator cg = new CodeGenerator( generatorConfig );
        cg.run();

    }

}
