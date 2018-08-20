import org.mybatis.generator.api.ShellRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {

    private final String generatorConfig;

    private boolean overwrite;

    public CodeGenerator(String generatorConfig) {
        this( generatorConfig, true );
    }

    public CodeGenerator(String generatorConfig, boolean overwrite) {
        this.overwrite = true;

        this.generatorConfig = generatorConfig;
        this.overwrite = overwrite;
    }

    public void run() {
        if (this.generatorConfig == null) {
            System.err.println( "generatorConfig不能为空!" );
            return;
        }
        File config = new File( this.generatorConfig );
        if ((!(config.exists())) || (!(config.canRead()))) {
            System.err.println( "配置文件不可用:" + this.generatorConfig );
            return;
        }
        List param = new ArrayList();
        param.add( "-configfile" );
        param.add( this.generatorConfig );
        if (this.overwrite) {
            param.add( "-overwrite" );
        }
        param.add( "-verbose" );

        String[] arg = (String[])param.toArray( new String[0] );
        ShellRunner.main( arg );
    }
}

