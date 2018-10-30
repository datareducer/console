<!DOCTYPE html>
<html lang="ru">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <title>${description}</title>

    <link rel="shortcut icon" href="/rapport/img/favicon.ico" />
    <link href="/rapport/css/bootstrap.min.css" rel="stylesheet">

  </head>

  <body>
  
    <main role="main" class="container">
        <h3>${name}</h3>

        <p><i>${description}</i></p>

        <#if dataFrame?size != 0>
        <table class="table table-sm mt-2">
            <thead>
                <tr>
                    <#list dataFrame[0]?keys as column>
                    <th>${column}</th>
                    </#list>
                </tr>
            <thead>
            <#list dataFrame as record>
                <tr>
                    <#list record?keys as column>
                    <#assign fld = record[column]>
                    <#if fld?is_boolean>
                    <td>${fld?string("Да","Нет")}</td>
                    <#else>
                    <td>${fld}</td>
                    </#if>
                    </#list>
                </tr>
            </#list>
        </table>
        </#if>
    </main>

    <!--script src="/rapport/js/jquery-3.3.1.min.js"></script-->
    <!--script src="/rapport/js/popper.min.js"></script-->
    <!--script src="/rapport/js/bootstrap.min.js"></script-->
    
  </body>
</html>
