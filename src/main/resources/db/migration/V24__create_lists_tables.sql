create table lists (
    id bigserial primary key,
    name varchar(150) not null,
    description text,
    active boolean not null default true,
    starts_at timestamp not null,
    ends_at timestamp not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint ck_lists_period check (ends_at >= starts_at)
);

create index idx_lists_active_period on lists (active, starts_at, ends_at);

create table list_items (
    id bigserial primary key,
    list_id bigint not null,
    name varchar(150) not null,
    description text,
    image_url varchar(500),
    total_quantity integer not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_list_items_list foreign key (list_id) references lists (id) on delete cascade,
    constraint ck_list_items_total_quantity check (total_quantity >= 1)
);

create index idx_list_items_list_id on list_items (list_id);
