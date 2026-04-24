create table public_list_submissions (
    id bigserial primary key,
    list_id bigint not null,
    full_name varchar(150) not null,
    phone varchar(30) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_public_list_submissions_list foreign key (list_id) references lists (id) on delete cascade
);

create index idx_public_list_submissions_list_id on public_list_submissions (list_id);
create index idx_public_list_submissions_created_at on public_list_submissions (created_at);

create table public_list_submission_items (
    id bigserial primary key,
    submission_id bigint not null,
    list_item_id bigint not null,
    quantity integer not null,
    constraint fk_public_list_submission_items_submission foreign key (submission_id) references public_list_submissions (id) on delete cascade,
    constraint fk_public_list_submission_items_list_item foreign key (list_item_id) references list_items (id) on delete restrict,
    constraint uk_public_list_submission_items_submission_item unique (submission_id, list_item_id),
    constraint ck_public_list_submission_items_quantity check (quantity >= 1)
);

create index idx_public_list_submission_items_submission_id on public_list_submission_items (submission_id);
create index idx_public_list_submission_items_list_item_id on public_list_submission_items (list_item_id);
