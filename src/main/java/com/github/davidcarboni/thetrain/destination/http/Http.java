package com.github.davidcarboni.thetrain.destination.http;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by david on 25/03/2015.
 */
public class Http implements AutoCloseable {

    private CloseableHttpClient httpClient;
    private ArrayList<Header> headers = new ArrayList<>();

    /**
     * Sends a GET request and returns the response.
     *
     * @param endpoint      The endpoint to send the request to.
     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param headers       Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param <T>           The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     */
    public <T> Response<T> get(Endpoint endpoint, Class<T> responseClass, NameValuePair... headers) throws IOException {

        // Create the request
        HttpGet get = new HttpGet(endpoint.url());
        get.setHeaders(combineHeaders(headers));


        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(get)) {
            //System.out.println(response);
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a GET request and returns the response.
     *
     * @param endpoint The endpoint to send the request to.
     * @param headers  Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @return A {@link Path} to the downloaded content, if any.
     * @throws IOException If an error occurs.
     * @see Files#probeContentType(Path)
     */
    public Response<Path> get(Endpoint endpoint, NameValuePair... headers) throws IOException {

        // Create the request
        HttpGet get = new HttpGet(endpoint.url());
        get.setHeaders(combineHeaders(headers));
        Path tempFile = null;

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(get)) {

            if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                return null;
            } // If bad response return null

            // Request the content
            HttpEntity entity = response.getEntity();

            // Download the content to a temporary file
            if (entity != null) {
                tempFile = Files.createTempFile("download", "file");
                try (InputStream input = entity.getContent();
                     OutputStream output = Files.newOutputStream(tempFile)) {
                    IOUtils.copy(input, output);
                }
            }

            return new Response<>(response.getStatusLine(), tempFile);
        }
    }

    /**
     * Sends a POST request and returns the response.
     *
     * @param endpoint       The endpoint to send the request to.
     * @param requestMessage A message to send in the request body. Can be null.
     * @param responseClass  The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param headers        Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param <T>            The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     */
    public <T> Response<T> post(Endpoint endpoint, Object requestMessage, Class<T> responseClass, NameValuePair... headers) throws IOException {
        if (requestMessage == null) {
            return post(endpoint, responseClass, headers);
        } // deal with null case

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());
        post.setHeaders(combineHeaders(headers));

        // Add the request message if there is one
        post.setEntity(serialiseRequestMessage(requestMessage));

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a POST request and returns the response.
     * <p/>
     * Specifically for the use case where we have no requestMessage
     *
     * @param endpoint      The endpoint to send the request to.
     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param headers       Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param <T>           The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     */
    public <T> Response<T> post(Endpoint endpoint, Class<T> responseClass, NameValuePair... headers) throws IOException {

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());
        post.setHeaders(combineHeaders(headers));

        // Add the request message if there is one
        post.setEntity(serialiseRequestMessage(null));

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a POST request with a file and returns the response.
     *
     * @param endpoint      The endpoint to send the request to.
     * @param file          The file to upload
     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param fields        Any name-value pairs to serialise
     * @param <T>           The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     * @see MultipartEntityBuilder
     */
    public <T> Response<T> post(Endpoint endpoint, File file, Class<T> responseClass, NameValuePair... fields) throws IOException {
        if (file == null) {
            return post(endpoint, responseClass, fields);
        } // deal with null case

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());
        post.setHeaders(combineHeaders());

        // Add fields as text pairs
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (NameValuePair field : fields) {
            multipartEntityBuilder.addTextBody(field.getName(), field.getValue());
        }
        // Add file as binary
        FileBody bin = new FileBody(file);
        multipartEntityBuilder.addPart("file", bin);

        // Set the body
        post.setEntity(multipartEntityBuilder.build());

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a POST request with a file and returns the response.
     *
     * @param endpoint      The endpoint to send the request to.
     * @param file          The file to upload
     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param <T>           The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     * @see MultipartEntityBuilder
     */
    public <T> Response<T> post(Endpoint endpoint, File file, Class<T> responseClass) throws IOException {
        if (file == null) {
            return post(endpoint, responseClass);
        } // deal with null case

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());
        post.setHeaders(combineHeaders());

        // Add fields as text pairs
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        // Add file as binary
        FileBody bin = new FileBody(file);
        multipartEntityBuilder.addPart("file", bin);

        // Set the body
        post.setEntity(multipartEntityBuilder.build());

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a POST request with a file and returns the response.
     *
     * @param endpoint      The endpoint to send the request to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     * @see MultipartEntityBuilder
     */
    public Response<Path> postAndReturn(Endpoint endpoint, Object requestMessage, NameValuePair... headers) throws IOException {
        Path tempFile = null;

        if (requestMessage == null) {
            return null;
        } // deal with null case

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());
        post.setHeaders(combineHeaders(headers));

        // Add the request message if there is one
        post.setEntity(serialiseRequestMessage(requestMessage));

       // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {

            if (response.getStatusLine().getStatusCode() != HttpStatus.OK_200) {
                return null;
            } // If bad response return null

            // Request the content
            HttpEntity entity = response.getEntity();

            // Download the content to a temporary file
            if (entity != null) {
                tempFile = Files.createTempFile("download", "file");
                try (InputStream input = entity.getContent();
                     OutputStream output = Files.newOutputStream(tempFile)) {
                    IOUtils.copy(input, output);
                }
            }

            return new Response<>(response.getStatusLine(), tempFile);
        }
    }

    /**
     * Sends a POST request and returns the response.
     *
     * @param endpoint       The endpoint to send the request to.
     * @param requestMessage A message to send in the request body. Can be null.
     * @param responseClass  The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param headers        Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param <T>            The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     */
    public <T> Response<T> put(Endpoint endpoint, Object requestMessage, Class<T> responseClass, NameValuePair... headers) throws IOException {

        // Create the request
        HttpPut put = new HttpPut(endpoint.url());
        put.setHeaders(combineHeaders(headers));

        // Add the request message if there is one
        put.setEntity(serialiseRequestMessage(requestMessage));

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(put)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Sends a POST request and returns the response.
     *
     * @param endpoint      The endpoint to send the request to.
     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
     * @param headers       Any additional headers to send with this request. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param <T>           The type to deserialise the response to.
     * @return A {@link Response} containing the deserialised body, if any.
     * @throws IOException If an error occurs.
     */
    public <T> Response<T> delete(Endpoint endpoint, Class<T> responseClass, NameValuePair... headers) throws IOException {

        // Create the request
        HttpDelete delete = new HttpDelete(endpoint.url());
        delete.setHeaders(combineHeaders(headers));

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(delete)) {
            T body = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), body);
        }
    }

    /**
     * Adds a header that will be used for all requests made by this instance.
     *
     * @param name  The header name. You can use {@link org.apache.http.HttpHeaders} constants for header names.
     * @param value The header value.
     */
    public void addHeader(String name, String value) {
        headers.add(new BasicHeader(name, value));
    }

    /**
     * Sets the combined request headers.
     *
     * @param headers Additional header values to add over and above {@link #headers}.
     */

    private Header[] combineHeaders(NameValuePair[] headers) {

        Header[] fullHeaders = new Header[this.headers.size() + headers.length];

        // Add class-level headers (for all requests)
        for (int i = 0; i < this.headers.size(); i++) {
            fullHeaders[i] = this.headers.get(i);
        }

        // Add headers specific to this request:
        for (int i = 0; i < headers.length; i++) {
            NameValuePair header = headers[i];
            fullHeaders[i + this.headers.size()] = new BasicHeader(header.getName(), header.getValue());
        }

        //System.out.println(Arrays.toString(fullHeaders));
        return fullHeaders;
    }

    private Header[] combineHeaders() {

        Header[] fullHeaders = new Header[this.headers.size()];

        // Add class-level headers (for all requests)
        for (int i = 0; i < this.headers.size(); i++) {
            fullHeaders[i] = this.headers.get(i);
        }

        //System.out.println(Arrays.toString(fullHeaders));
        return fullHeaders;
    }

    /**
     * Serialises the given object as a {@link StringEntity}.
     *
     * @param requestMessage The object to be serialised.
     * @throws UnsupportedEncodingException If a serialisation error occurs.
     */
    private StringEntity serialiseRequestMessage(Object requestMessage) throws UnsupportedEncodingException {
        StringEntity result = null;

        // Add the request message if there is one
        if (requestMessage != null) {
            // Send the message
            String message = Serialiser.serialise(requestMessage);
            result = new StringEntity(message);
        }

        return result;
    }

    /**
     * Deserialises the given {@link CloseableHttpResponse} to the specified type.
     *
     * @param response      The response.
     * @param responseClass The type to deserialise to.
     * @param <T>           The type to deserialise to.
     * @return The deserialised response, or null if the response does not contain an entity.
     * @throws IOException If an error occurs.
     */
    private <T> T deserialiseResponseMessage(CloseableHttpResponse response, Class<T> responseClass) throws IOException {
        T body = null;

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (InputStream inputStream = entity.getContent()) {
                try {
                    body = Serialiser.deserialise(inputStream, responseClass);
                } catch (JsonSyntaxException e) {
                    // This can happen if an error HTTP code is received and the
                    // body of the response doesn't contain the expected object:
                    body = null;
                }
            }
        } else {
            EntityUtils.consume(entity);
        }

        return body;
    }

    private CloseableHttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
        return httpClient;
    }

    @Override
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // Mostly ignore it
                e.printStackTrace();
            }
        }
    }

}
