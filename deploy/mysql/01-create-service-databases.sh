#!/bin/bash
set -euo pipefail

mysql=(mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}")
for database in delivery_identity delivery_catalog delivery_cart delivery_order delivery_media; do
  "${mysql[@]}" -e "CREATE DATABASE IF NOT EXISTS \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
done

sql_quote() { printf "%s" "$1" | sed "s/'/''/g"; }
create_user() {
  local username password
  username=$(sql_quote "$1")
  password=$(sql_quote "$2")
  "${mysql[@]}" -e "CREATE USER IF NOT EXISTS '${username}'@'%'; ALTER USER '${username}'@'%' IDENTIFIED BY '${password}';"
}

# Each extracted service uses a distinct database user so it cannot query or
# write another service's tables.
create_user "${IDENTITY_DB_USERNAME}" "${IDENTITY_DB_PASSWORD}"
create_user "${CATALOG_DB_USERNAME}" "${CATALOG_DB_PASSWORD}"
create_user "${CART_DB_USERNAME}" "${CART_DB_PASSWORD}"
create_user "${ORDER_DB_USERNAME}" "${ORDER_DB_PASSWORD}"
create_user "${MEDIA_DB_USERNAME}" "${MEDIA_DB_PASSWORD}"

"${mysql[@]}" -e "
  GRANT ALL PRIVILEGES ON delivery_identity.* TO '${IDENTITY_DB_USERNAME}'@'%';
  GRANT ALL PRIVILEGES ON delivery_catalog.* TO '${CATALOG_DB_USERNAME}'@'%';
  GRANT ALL PRIVILEGES ON delivery_cart.* TO '${CART_DB_USERNAME}'@'%';
  GRANT ALL PRIVILEGES ON delivery_order.* TO '${ORDER_DB_USERNAME}'@'%';
  GRANT ALL PRIVILEGES ON delivery_media.* TO '${MEDIA_DB_USERNAME}'@'%';
  FLUSH PRIVILEGES;"
