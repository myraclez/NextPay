This plugin is advertised as standalone, even though it only works with Vault, this is because it uses vault as a bridge to other plugins in order to have a shared economy.


Access the API:

Put depend: [ NextPay ] 
inside your plugin.yml

maven:
          <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>

as repository

        <dependency>
            <groupId>com.github.myraclez.Nextpay</groupId>
            <artifactId>nextpay-api</artifactId>
            <version>v1.0.2</version>
            <scope>provided</scope>
        </dependency>

        as dependncy

