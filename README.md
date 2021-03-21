# lab3-infracom

Se debe de descargar el ejecutable que lo encontraran en el último release de este repositorio.

### Lab3-Infracom.jar

Con este archivo es posible ejecutar tanto el servidor como cada cliente.
Adicional se debe de asegurar que la maquina en donde se ejecutara este archivo debe de tener Java instalado.

## Servidor
Desde el servidor deberán de ejecutar el siguiente comando 

```java -cp Lab3-Infracom.jar Server "{Archivo a enviar}" {Número de clientes}```

Deben de reemplazar los parámetros ```{Archivo a enviar}``` por la ruta del archivo que se desea enviar a cada cliente. Y ```{Número de clientes}``` con la cantidad de clientes ha de esperar hasta que el servidor empiece él envió de archivo.

## Cliente

Desde cada cliente deberá de ejecutar el siguiente comando

```java -cp Lab3-Infracom.jar Client "{Ruta de almacenamiento}" "{IP del servidor}" "{Numero de la prueba}"```

Deben de reemplazar los parámetros ```{Ruta de almacenamiento}``` por la ruta en donde se desea almacenar los archivos a recibir. Y ```{Numero de la prueba}``` representará el número de clientes a probar, este último parámetro solo será usado para almacenar el archivo con el nombre adecuado.
