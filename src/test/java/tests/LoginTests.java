package tests;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Listeners;
import app.getxray.xray.testng.annotations.XrayTest;
import app.getxray.xray.testng.annotations.Requirement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import pages.LoginPage;
import pages.LoginResultsPage;
import util.RepositoryParser;

import java.time.Duration;

@Listeners({ app.getxray.xray.testng.listeners.XrayListener.class })
public class LoginTests {
    WebDriver driver;
    RepositoryParser repo;

    @BeforeSuite
    public void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox"); // Bypass OS security model, to run in Docker
        options.addArguments("--headless");
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
        driver.manage().window().maximize();
        repo = new RepositoryParser("./src/configs/object.properties");
    }

    @AfterSuite
    public void tearDown() throws Exception {
        driver.quit();
    }

    @Test
    @XrayTest(key = "TES-3")
    public void validLogin() {
        LoginPage loginPage = new LoginPage(driver).open();
        Assert.assertTrue(loginPage.isVisible());
        LoginResultsPage loginResultsPage = loginPage.login("admin", "admin");
        Assert.assertEquals(loginResultsPage.getTitle(), repo.getBy("expected.login.title"));
    }

    @Test
    @XrayTest(key = "TES-4", summary = "invalid login test", description = "login attempt with invalid credentials", labels = "authentication")
    public void invalidLogin() {
        LoginPage loginPage = new LoginPage(driver).open();
        Assert.assertTrue(loginPage.isVisible());
        LoginResultsPage loginResultsPage = loginPage.login("demo", "invalid");
        Assert.assertEquals(loginResultsPage.getTitle(), repo.getBy("expected.error.title"));
        Assert.assertTrue(loginResultsPage.contains(repo.getBy("expected.login.failed")));
    }
    @Test
    @XrayTest(key = "TES-8")
    public void inValidLoginTest() {
        System.out.println("I am in valid login test");
    }

}
