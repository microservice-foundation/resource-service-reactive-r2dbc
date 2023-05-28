//package com.epam.training.microservicefoundation.resourceservice.base;

//@SpringBootTest(classes = ResourceServiceApplication.class)
//@DirtiesContext
//@ExtendWith(value = {PostgresExtension.class})
//@ContextConfiguration(classes = DatasourceConfiguration.class)
//@TestPropertySource(locations = "classpath:application.properties")
//public abstract class RestBase {
//  @MockBean
//  ResourceService service;
//  @BeforeEach
//  public void setup(ApplicationContext context) throws IOException {
//    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
//    GetObjectResponse getObjectResponse = GetObjectResponse.builder()
//        .contentType(MediaType.APPLICATION_OCTET_STREAM.toString())
//        .contentLength(path.toFile().length())
//        .build();
//
//    SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
//        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");
//
//    when(service.getById(123L)).thenReturn(Mono.just(new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher)));
//    when(service.deleteByIds(Flux.empty()))
//        .thenReturn(Flux.error(new IllegalArgumentException("Id param is not validated, check your ids")));
//
//    ArgumentCaptor<Flux<Long>> fluxArgumentCaptor = ArgumentCaptor.forClass(Flux.class);
//    when(service.deleteByIds(fluxArgumentCaptor.capture())).thenReturn(Flux.just(new ResourceRecord(123L)));
//    when(service.getById(1999L)).thenReturn(Mono.error(new ResourceNotFoundException("Resource is not found with id '1999'")));

//    RestAssuredWebTestClient.applicationContextSetup(context);
//  }
//}
