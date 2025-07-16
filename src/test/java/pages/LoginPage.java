package pages;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.By;
import util.RepositoryParser;

public class LoginPage {

    private WebDriver driver;
    private RepositoryParser repo;
    private WebElement usernameElement;
    private WebElement passwordElement;
    private WebElement submitButtonElement;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        repo = new RepositoryParser("./src/configs/object.properties");
        PageFactory.initElements(driver, this);
    }

    public LoginPage open()
    {
        driver.navigate().to(repo.getBy("url"));
        return this;
    }

    public void setUsername(String username) {
        usernameElement = driver.findElement(By.name(repo.getBy("username.field.name")));
        usernameElement.sendKeys(username);
    }

    public void setPassword(String password) {
        passwordElement = driver.findElement(By.name(repo.getBy("password.field.name")));
        passwordElement.sendKeys(password);
    }

    public WebElement getSubmitButton(){
        submitButtonElement = driver.findElement(By.name(repo.getBy("login.button.name")));
        return submitButtonElement;
    }

    public LoginResultsPage submit()
    {
       getSubmitButton().submit();
       return new LoginResultsPage(driver);
    }

    public LoginResultsPage login(String username, String password)
    {
        setUsername(username);
        setPassword(password);
        return submit();
    }

    public Boolean contains(String text) {
        return driver.getPageSource().contains(text);
    }

    public String getTitle()
    {

        return driver.getTitle();
    }

    public Boolean isVisible()
    {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        return wait.until(ExpectedConditions.elementToBeClickable(getSubmitButton())).isDisplayed();
    }

}