package gcms.youtube2podcast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
/**
 * Created by gustavo on 19/02/17.
 */
@EnableWebMvc
@Configurable
@ComponentScan
class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        File files = new File("/tmp/files/")
//        files.mkdirs()
//        registry.addResourceHandler("/file/*").
//                addResourceLocations(files.toURI().toString())
    }
}
