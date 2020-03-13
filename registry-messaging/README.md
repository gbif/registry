# GBIF Registry Messaging

This module includes messaging related functionality.
It uses [gbif-postal-service](https://github.com/gbif/postal-service) internally to create an instance of `MessagePublisher` interface.
[RabbitMQ](https://www.rabbitmq.com/) is a message broker.

## Configuration

* `spring.rabbitmq.host` RabbitMQ host (e.g. localhost)
* `spring.rabbitmq.port` RabbitMQ port (default is 5672)
* `spring.rabbitmq.username` Login user to authenticate to the broker
* `spring.rabbitmq.password` Login to authenticate against the broker
* `spring.rabbitmq.virtual-host` Virtual host to use when connecting to the broker (e.g. /dev)
* `message.enabled` disable messaging if false

[Parent](../README.md)
