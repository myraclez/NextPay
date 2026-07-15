This plugin is advertised as standalone, even though it only works with Vault, this is because it uses vault as a bridge to other plugins in order to have a shared economy.

Download: https://builtbybit.com/resources/nextpay-payments-the-modern-way.116018/

Access the API:

Put depend: [ NextPay ] 
inside your plugin.yml


        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>

        <dependency>
            <groupId>com.github.myraclez.Nextpay</groupId>
            <artifactId>nextpay-api</artifactId>
            <version>tag</version>
            <scope>provided</scope>
        </dependency>

All methods are accessed through NextPayAPI interface.

To get to the methods:
    NextPayAPI.get().<method>
