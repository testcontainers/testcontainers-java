import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BaiduSearchTest {
    
    @Container
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
            .withCapabilities(new ChromeOptions());
    
    @Test
    void testNormalSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入"Testcontainers"
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("Testcontainers");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证搜索结果是否包含"Testcontainers"
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).contains("Testcontainers");
        
        driver.quit();
    }
    
    @Test
    void testEmptySearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入空字符串
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证是否没有搜索结果或显示提示信息
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).isEmpty();
        
        driver.quit();
    }
    
    @Test
    void testSpecialCharactersSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入特殊字符
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("!@#$%^&*()_+{}[]|\\:;'\"<>,.?/");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证搜索结果是否包含特殊字符或显示提示信息
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).contains("!@#$%^&*");
        
        driver.quit();
    }
    
    @Test
    void testLongStringSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入非常长的字符串
        WebElement searchInput = driver.findElement(By.id("kw"));
        String longString = "a".repeat(1000);
        searchInput.sendKeys(longString);
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证搜索结果是否包含长字符串的部分内容或显示提示信息
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).contains("aaaaa");
        
        driver.quit();
    }
    
    @Test
    void testSpaceSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入空格
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("   ");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证是否没有搜索结果或显示提示信息
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).isEmpty();
        
        driver.quit();
    }
    
    @Test
    void testEnglishCharactersSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入英文字符
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("Hello World");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证搜索结果是否包含英文字符
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).contains("Hello World");
        
        driver.quit();
    }
    
    @Test
    void testSqlInjectionSearch() {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        
        // 打开百度网站
        driver.get("https://www.baidu.com/?a");
        
        // 找到搜索输入框并输入SQL注入语句
        WebElement searchInput = driver.findElement(By.id("kw"));
        searchInput.sendKeys("' OR 1=1 --");
        
        // 按下回车键进行搜索
        searchInput.sendKeys(Keys.RETURN);
        
        // 验证搜索结果是否正常显示或显示错误信息
        WebElement searchResult = driver.findElement(By.id("content_left"));
        assertThat(searchResult.getText()).isNotEmpty();
        
        driver.quit();
    }
}