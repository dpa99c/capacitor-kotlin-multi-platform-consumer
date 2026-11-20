export interface KmpConsumerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
