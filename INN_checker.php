<html>
    <style>
        h1 {
            font-size: 300%; 
        }
    </style>
    <head>
        <meta charset="utf-8">
        <title>Finding data abount bank card with IIN</title>
        <h1>Finding data abount bank card with IIN</h1>
    </head>
    <body bgcolor="FFDAB9">
        <form action="INN_data_parser.php" method="post">
            <label style="margin-left: 50px; font-size: 250%; line-height: 1.5" for="name">Enter IIN (first six or eight digits of bank card number)<br></label>
            <input style="margin-left: 100px; font-size: 200%; width: 152px;" name="IIN" type="text" maxlength="8" class="input-number" placeholder="00000000">
            <input style="margin-left: 20px; font-size: 200%;" type="submit" value="Check">
        </form>
            <p style="font-size: 200%;" align = "right">
                Some IINs in database<br>
                <?php
                    $output=null;
                    $retval=null;
                    exec("java getLastDBLines", $output, $retval);
                    echo $output[0];
                ?>
            </p>
    </body>
</html>