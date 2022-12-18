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
    <body bgcolor="#FFDAB9">
        <?php
            $IIN = !empty($_POST['IIN']) ? $_POST['IIN'] : '';
            $output=null;
            $retval=null;
            exec("java Main ".$IIN, $output, $retval);
            echo $output[0];
        ?>
    </body>
</html>