FROM php:8.3-apache

RUN apt-get update \
    && apt-get install -y --no-install-recommends libonig-dev libcurl4-openssl-dev \
    && docker-php-ext-install pdo pdo_mysql mbstring curl \
    && rm -rf /var/lib/apt/lists/*

RUN a2enmod rewrite headers

RUN echo "ServerName localhost" >> /etc/apache2/apache2.conf

COPY docker/apache/000-default.conf /etc/apache2/sites-available/000-default.conf

WORKDIR /var/www/html
